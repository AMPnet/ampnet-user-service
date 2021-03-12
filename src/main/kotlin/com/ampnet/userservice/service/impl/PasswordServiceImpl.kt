package com.ampnet.userservice.service.impl

import com.ampnet.userservice.amqp.mailservice.MailService
import com.ampnet.userservice.amqp.mailservice.UserDataWithToken
import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InternalException
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.persistence.model.ForgotPasswordToken
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.persistence.repository.ForgotPasswordTokenRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.PasswordService
import com.ampnet.userservice.service.pojo.UserResponse
import mu.KLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class PasswordServiceImpl(
    private val userRepository: UserRepository,
    private val forgotPasswordTokenRepository: ForgotPasswordTokenRepository,
    private val coopRepository: CoopRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailService: MailService,
    private val applicationProperties: ApplicationProperties
) : PasswordService {

    companion object : KLogging()

    @Transactional
    @Throws(InvalidRequestException::class)
    override fun changePassword(userUuid: UUID, oldPassword: String, newPassword: String): UserResponse {
        val user = ServiceUtils.wrapOptional(userRepository.findById(userUuid))
            ?: throw InvalidRequestException(ErrorCode.USER_JWT_MISSING, "Cannot change password")
        if (user.authMethod != AuthMethod.EMAIL) {
            throw InvalidRequestException(ErrorCode.AUTH_INVALID_LOGIN_METHOD, "Cannot change password")
        }
        if (passwordEncoder.matches(oldPassword, user.password).not()) {
            throw InvalidRequestException(ErrorCode.USER_DIFFERENT_PASSWORD, "Invalid old password")
        }
        logger.info { "Changing password for user: ${user.uuid}" }
        user.password = passwordEncoder.encode(newPassword)
        userRepository.save(user)
        return generateUserResponse(user)
    }

    @Transactional
    @Throws(ResourceNotFoundException::class, InternalException::class)
    override fun changePasswordWithToken(token: UUID, newPassword: String): UserResponse {
        val forgotToken = forgotPasswordTokenRepository.findByToken(token).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.AUTH_FORGOT_TOKEN_MISSING, "Missing forgot token: $token")
        }
        if (forgotToken.isExpired()) {
            throw InvalidRequestException(ErrorCode.AUTH_FORGOT_TOKEN_EXPIRED, "Expired token: $token")
        }
        val user = forgotToken.user
        forgotPasswordTokenRepository.delete(forgotToken)
        user.password = passwordEncoder.encode(newPassword)
        logger.info { "Changing password using forgot password token for user: ${user.email}" }
        return generateUserResponse(user)
    }

    @Transactional
    @Throws(InvalidRequestException::class)
    override fun generateForgotPasswordToken(email: String, coop: String?): Boolean {
        val coopOrDefault = coop ?: applicationProperties.coop.default
        val user = ServiceUtils.wrapOptional(userRepository.findByCoopAndEmail(coopOrDefault, email))
            ?: return false
        if (user.authMethod != AuthMethod.EMAIL) {
            throw InvalidRequestException(ErrorCode.AUTH_INVALID_LOGIN_METHOD, "Cannot change password")
        }
        logger.info { "Generating forgot password token for user: ${user.email}" }
        val forgotPasswordToken = ForgotPasswordToken(0, user, UUID.randomUUID(), ZonedDateTime.now())
        forgotPasswordTokenRepository.save(forgotPasswordToken)
        mailService.sendResetPasswordMail(UserDataWithToken(user, forgotPasswordToken.token))
        return true
    }

    override fun verifyPasswords(password: String, encodedPassword: String?): Boolean =
        encodedPassword?.let { passwordEncoder.matches(password, encodedPassword) } ?: false

    private fun generateUserResponse(user: User): UserResponse {
        val coop = coopRepository.findByIdentifier(user.coop)
            ?: throw InvalidRequestException(ErrorCode.COOP_MISSING, "Missing coop")
        return UserResponse(user, coop.needUserVerification)
    }
}
