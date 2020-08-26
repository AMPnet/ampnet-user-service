package com.ampnet.userservice.service.impl

import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.grpc.mailservice.MailService
import com.ampnet.userservice.persistence.model.ForgotPasswordToken
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.ForgotPasswordTokenRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.PasswordService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class PasswordServiceImpl(
    private val userRepository: UserRepository,
    private val forgotPasswordTokenRepository: ForgotPasswordTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailService: MailService
) : PasswordService {

    @Transactional
    override fun changePassword(user: User, oldPassword: String, newPassword: String): User {
        if (user.authMethod != AuthMethod.EMAIL) {
            throw InvalidRequestException(ErrorCode.AUTH_INVALID_LOGIN_METHOD, "Cannot change password")
        }
        if (passwordEncoder.matches(oldPassword, user.password).not()) {
            throw InvalidRequestException(ErrorCode.USER_DIFFERENT_PASSWORD, "Invalid old password")
        }
        UserServiceImpl.logger.info { "Changing password for user: ${user.uuid}" }
        user.password = passwordEncoder.encode(newPassword)
        return userRepository.save(user)
    }

    @Transactional
    override fun changePasswordWithToken(token: UUID, newPassword: String): User {
        val forgotToken = forgotPasswordTokenRepository.findByToken(token).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.AUTH_FORGOT_TOKEN_MISSING, "Missing forgot token: $token")
        }
        if (forgotToken.isExpired()) {
            throw InvalidRequestException(ErrorCode.AUTH_FORGOT_TOKEN_EXPIRED, "Expired token: $token")
        }
        val user = forgotToken.user
        forgotPasswordTokenRepository.delete(forgotToken)
        user.password = passwordEncoder.encode(newPassword)
        UserServiceImpl.logger.info { "Changing password using forgot password token for user: ${user.email}" }
        return user
    }

    @Transactional
    override fun generateForgotPasswordToken(email: String): Boolean {
        val optionalUser = userRepository.findByEmail(email)
        if (optionalUser.isPresent.not()) {
            return false
        }
        val user = optionalUser.get()
        if (user.authMethod != AuthMethod.EMAIL) {
            throw InvalidRequestException(ErrorCode.AUTH_INVALID_LOGIN_METHOD, "Cannot change password")
        }
        UserServiceImpl.logger.info { "Generating forgot password token for user: ${user.email}" }
        val forgotPasswordToken = ForgotPasswordToken(0, user, UUID.randomUUID(), ZonedDateTime.now())
        forgotPasswordTokenRepository.save(forgotPasswordToken)
        mailService.sendResetPasswordMail(user.email, forgotPasswordToken.token.toString())
        return true
    }

    override fun verifyPasswords(password: String, encodedPassword: String?): Boolean {
        encodedPassword?.let {
            return passwordEncoder.matches(password, encodedPassword)
        }
        return false
    }
}
