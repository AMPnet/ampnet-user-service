package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.grpc.mailservice.MailService
import com.ampnet.userservice.persistence.model.MailToken
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.persistence.repository.MailTokenRepository
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
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
    private val userInfoRepository: UserInfoRepository,
    private val mailTokenRepository: MailTokenRepository,
    private val coopRepository: CoopRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
    private val applicationProperties: ApplicationProperties
) : UserService {

    companion object : KLogging()

    @Transactional
    override fun createUser(request: CreateUserServiceRequest): User {
        if (coopRepository.findByIdentifier(request.coop).isPresent.not()) {
            throw ResourceNotFoundException(ErrorCode.REG_COOP_MISSING, "Missing coop with identifier: ${request.coop}")
        }
        if (userRepository.findByEmailAndCoop(request.email, request.coop).isPresent) {
            throw ResourceAlreadyExistsException(
                ErrorCode.REG_USER_EXISTS,
                "Trying to create user with email that already exists: ${request.email} in coop: ${request.coop}"
            )
        }
        val user = createUserFromRequest(request)
        if (user.authMethod == AuthMethod.EMAIL && user.enabled.not()) {
            val mailToken = createMailToken(user)
            mailService.sendConfirmationMail(user.email, mailToken.token.toString())
        }
        if (applicationProperties.user.firstAdmin && userRepository.countByCoop(request.coop) == 1L) {
            user.role = UserRole.ADMIN
        }
        logger.info { "Created user: ${user.email}" }
        return user
    }

    @Transactional
    override fun connectUserInfo(userUuid: UUID, clientSessionUuid: String): User {
        val user = find(userUuid)
            ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "Missing user with uuid: $userUuid")
        val userInfo = userInfoRepository.findByClientSessionUuid(clientSessionUuid).orElseThrow {
            throw ResourceNotFoundException(
                ErrorCode.REG_IDENTYUM,
                "Missing UserInfo with Identyum clientSessionUuid(sessionState): $clientSessionUuid"
            )
        }
        userInfo.connected = true
        user.userInfoId = userInfo.id
        user.firstName = userInfo.firstName
        user.lastName = userInfo.lastName
        logger.info { "Connected UserInfo: ${userInfo.id} to user: ${user.uuid}" }
        return user
    }

    @Transactional(readOnly = true)
    override fun find(email: String, coop: String): User? {
        return ServiceUtils.wrapOptional(userRepository.findByEmailAndCoop(email, coop))
    }

    @Transactional(readOnly = true)
    override fun find(userUuid: UUID): User? {
        return ServiceUtils.wrapOptional(userRepository.findById(userUuid))
    }

    @Transactional
    override fun confirmEmail(token: UUID): User? {
        ServiceUtils.wrapOptional(mailTokenRepository.findByToken(token))?.let { mailToken ->
            if (mailToken.isExpired()) {
                throw InvalidRequestException(
                    ErrorCode.REG_EMAIL_EXPIRED_TOKEN,
                    "User is trying to confirm mail with expired token: $token"
                )
            }
            val user = mailToken.user
            user.enabled = true
            mailTokenRepository.delete(mailToken)
            logger.debug { "Email confirmed for user: ${user.email}" }
            return user
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
        val user = User(
            UUID.randomUUID(),
            request.firstName,
            request.lastName,
            request.email,
            null,
            request.authMethod,
            null,
            UserRole.USER,
            ZonedDateTime.now(),
            true,
            request.coop
        )
        if (request.authMethod == AuthMethod.EMAIL) {
            user.enabled = applicationProperties.mail.confirmationNeeded.not()
            user.password = passwordEncoder.encode(request.password.orEmpty())
        }
        return userRepository.save(user)
    }

    private fun createMailToken(user: User): MailToken {
        val mailToken = MailToken(0, user, UUID.randomUUID(), ZonedDateTime.now())
        return mailTokenRepository.save(mailToken)
    }
}
