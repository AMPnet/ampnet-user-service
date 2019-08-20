package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.persistence.model.MailToken
import com.ampnet.userservice.persistence.model.Role
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.MailTokenRepository
import com.ampnet.userservice.persistence.repository.RoleRepository
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.MailService
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import mu.KLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userInfoRepository: UserInfoRepository,
    private val mailTokenRepository: MailTokenRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
    private val applicationProperties: ApplicationProperties
) : UserService {

    companion object : KLogging()

    private val userRole: Role by lazy { roleRepository.getOne(UserRoleType.USER.id) }

    @Transactional
    override fun createUser(request: CreateUserServiceRequest): User {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.REG_USER_EXISTS,
                "Trying to create user with email that already exists: ${request.email}")
        }

        val user = createUserFromRequest(request)
        userRepository.save(user)
        if (user.authMethod == AuthMethod.EMAIL && user.enabled.not()) {
            val mailToken = createMailToken(user)
            mailService.sendConfirmationMail(user.email, mailToken.token.toString())
        }
        return user
    }

    @Transactional(readOnly = true)
    override fun find(email: String): User? {
        return ServiceUtils.wrapOptional(userRepository.findByEmail(email))
    }

    @Transactional(readOnly = true)
    override fun find(userUuid: UUID): User? {
        return ServiceUtils.wrapOptional(userRepository.findById(userUuid))
    }

    @Transactional
    override fun confirmEmail(token: UUID): User? {
        ServiceUtils.wrapOptional(mailTokenRepository.findByToken(token))?.let { mailToken ->
            if (mailToken.isExpired()) {
                throw InvalidRequestException(ErrorCode.REG_EMAIL_EXPIRED_TOKEN,
                    "User is trying to confirm mail with expired token: $token")
            }
            val user = mailToken.user
            user.enabled = true

            mailTokenRepository.delete(mailToken)
            return userRepository.save(user)
        }
        return null
    }

    @Transactional
    override fun resendConfirmationMail(user: User) {
        if (user.authMethod != AuthMethod.EMAIL) {
            return
        }

        mailTokenRepository.findByUserUuid(user.uuid).ifPresent {
            mailTokenRepository.delete(it)
        }
        val mailToken = createMailToken(user)
        mailService.sendConfirmationMail(user.email, mailToken.token.toString())
    }

    private fun createUserFromRequest(request: CreateUserServiceRequest): User {
        val userInfo = userInfoRepository.findByWebSessionUuid(request.webSessionUuid).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.REG_IDENTYUM,
                "Missing UserInfo with Identyum webSessionUuid: ${request.webSessionUuid}")
        }
        val user = User::class.java.getDeclaredConstructor().newInstance().apply {
            this.uuid = UUID.randomUUID()
            this.email = request.email
            this.authMethod = request.authMethod
            this.createdAt = ZonedDateTime.now()
            this.role = userRole
            this.userInfo = userInfo
            this.userInfo.connected = true
            this.enabled = true
        }
        if (request.authMethod == AuthMethod.EMAIL) {
            user.enabled = applicationProperties.mail.confirmationNeeded.not()
            user.password = passwordEncoder.encode(request.password.orEmpty())
        }
        return user
    }

    private fun createMailToken(user: User): MailToken {
        val mailToken = MailToken::class.java.getConstructor().newInstance().apply {
            this.user = user
            token = UUID.randomUUID()
            createdAt = ZonedDateTime.now()
        }
        return mailTokenRepository.save(mailToken)
    }
}
