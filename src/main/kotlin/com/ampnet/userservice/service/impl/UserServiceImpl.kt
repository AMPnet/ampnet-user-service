package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.request.UserUpdateRequest
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
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
    override fun createUser(request: CreateUserServiceRequest): User {
        val coop = getCoop(request.coop)
        if (coopRepository.findByIdentifier(coop) == null) {
            throw ResourceNotFoundException(ErrorCode.COOP_MISSING, "Missing coop with identifier: $coop")
        }
        if (userRepository.findByCoopAndEmail(coop, request.email).isPresent) {
            throw ResourceAlreadyExistsException(
                ErrorCode.REG_USER_EXISTS,
                "Trying to create user with email that already exists: ${request.email} in coop: $coop"
            )
        }
        val user = createUserFromRequest(request)
        if (user.authMethod == AuthMethod.EMAIL && user.enabled.not()) {
            userMailService.sendMailConfirmation(user)
        }
        if (applicationProperties.user.firstAdmin && userRepository.countByCoop(coop) == 1L) {
            user.role = UserRole.ADMIN
        }
        logger.info { "Created user: ${user.email}" }
        return user
    }

    @Transactional
    override fun connectUserInfo(userUuid: UUID, sessionId: String): User {
        val user = getUser(userUuid)
        val userInfo = userInfoRepository.findBySessionId(sessionId).orElseThrow {
            throw ResourceNotFoundException(
                ErrorCode.REG_VERIFF,
                "Missing UserInfo with Veriff session id: $sessionId"
            )
        }
        userInfo.connected = true
        user.userInfoUuid = userInfo.uuid
        user.firstName = userInfo.firstName
        user.lastName = userInfo.lastName
        logger.info { "Connected UserInfo: ${userInfo.uuid} to user: ${user.uuid}" }
        return user
    }

    @Transactional(readOnly = true)
    override fun find(email: String, coop: String?): User? =
        ServiceUtils.wrapOptional(userRepository.findByCoopAndEmail(getCoop(coop), email))

    @Transactional(readOnly = true)
    override fun find(userUuid: UUID): User? = ServiceUtils.wrapOptional(userRepository.findById(userUuid))

    @Transactional(readOnly = true)
    override fun countAllUsers(coop: String?): Int = userRepository.countByCoop(getCoop(coop)).toInt()

    @Transactional
    @Throws(ResourceNotFoundException::class)
    override fun update(userUuid: UUID, request: UserUpdateRequest): User {
        val user = getUser(userUuid)
        user.language = request.language
        return user
    }

    private fun getUser(userUuid: UUID): User = find(userUuid)
        ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "Missing user with uuid: $userUuid")

    private fun getCoop(coop: String?) = coop ?: applicationProperties.coop.default

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
            getCoop(request.coop),
            null
        )
        if (request.authMethod == AuthMethod.EMAIL) {
            user.enabled = applicationProperties.mail.confirmationNeeded.not()
            user.password = passwordEncoder.encode(request.password.orEmpty())
        }
        return userRepository.save(user)
    }
}
