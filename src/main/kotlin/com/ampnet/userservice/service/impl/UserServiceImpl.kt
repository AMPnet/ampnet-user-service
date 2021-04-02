package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.request.UserUpdateRequest
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.UserMailService
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
    private val coopRepository: CoopRepository,
    private val userMailService: UserMailService,
    private val passwordEncoder: PasswordEncoder,
    private val applicationProperties: ApplicationProperties
) : UserService {

    companion object : KLogging()

    @Transactional
    @Throws(ResourceNotFoundException::class, ResourceAlreadyExistsException::class, InvalidRequestException::class)
    override fun createUser(request: CreateUserServiceRequest): User {
        val coopIdentifier = getCoopIdentifier(request.coop)
        coopRepository.findByIdentifier(coopIdentifier)?.let {
            if (it.disableSignUp) {
                throw InvalidRequestException(
                    ErrorCode.REG_SIGNUP_DISABLED, "Signup is disabled for coop: $coopIdentifier"
                )
            }
        } ?: throw ResourceNotFoundException(ErrorCode.COOP_MISSING, "Missing coop with identifier: $coopIdentifier")
        if (userRepository.findByCoopAndEmail(coopIdentifier, request.email).isPresent) {
            throw ResourceAlreadyExistsException(
                ErrorCode.REG_USER_EXISTS,
                "Trying to create user with email that already exists: ${request.email} in coop: $coopIdentifier"
            )
        }
        val user = createUserFromRequest(request)
        if (user.authMethod == AuthMethod.EMAIL && user.enabled.not()) {
            userMailService.sendMailConfirmation(user)
        }
        if (applicationProperties.user.firstAdmin && userRepository.countByCoop(coopIdentifier) == 1L) {
            user.role = UserRole.ADMIN
        }
        logger.info { "Created user: ${user.email}" }
        return user
    }

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun connectUserInfo(userUuid: UUID, sessionId: String): User {
        val userInfo = userInfoRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).firstOrNull()
            ?: throw ResourceNotFoundException(ErrorCode.REG_INCOMPLETE, "Missing UserInfo with session id: $sessionId")
        val user = getUser(userUuid)
        disconnectUserInfo(user)
        userInfo.connected = true
        user.userInfoUuid = userInfo.uuid
        user.firstName = userInfo.firstName
        user.lastName = userInfo.lastName
        logger.info { "Connected UserInfo: ${userInfo.uuid} to user: ${user.uuid}" }
        return user
    }

    @Transactional(readOnly = true)
    override fun find(email: String, coop: String?): User? =
        ServiceUtils.wrapOptional(userRepository.findByCoopAndEmail(getCoopIdentifier(coop), email))

    @Transactional(readOnly = true)
    override fun find(userUuid: UUID): User? = ServiceUtils.wrapOptional(userRepository.findById(userUuid))

    @Transactional(readOnly = true)
    override fun countAllUsers(coop: String?): Int = userRepository.countByCoop(getCoopIdentifier(coop)).toInt()

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun update(userUuid: UUID, request: UserUpdateRequest): User {
        val user = getUser(userUuid)
        user.language = request.language
        return user
    }

    private fun disconnectUserInfo(user: User) {
        user.userInfoUuid?.let {
            userInfoRepository.findById(it).ifPresent { userInfo ->
                userInfo.connected = false
                userInfo.deactivated = true
                userInfoRepository.save(userInfo)
                logger.info { "Disconnected old user info: ${userInfo.uuid} for user: ${user.uuid}" }
            }
        }
    }

    private fun getUser(userUuid: UUID): User = find(userUuid)
        ?: throw ResourceNotFoundException(ErrorCode.USER_JWT_MISSING, "Missing user with uuid: $userUuid")

    private fun getCoopIdentifier(coop: String?) = coop ?: applicationProperties.coop.default

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
            getCoopIdentifier(request.coop),
            null
        )
        if (request.authMethod == AuthMethod.EMAIL) {
            user.enabled = applicationProperties.mail.confirmationNeeded.not()
            user.password = passwordEncoder.encode(request.password.orEmpty())
        }
        return userRepository.save(user)
    }
}
