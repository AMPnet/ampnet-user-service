package com.ampnet.userservice.controller

import com.ampnet.core.jwt.exception.TokenException
import com.ampnet.userservice.controller.pojo.request.ChangePasswordTokenRequest
import com.ampnet.userservice.controller.pojo.request.MailCheckRequest
import com.ampnet.userservice.controller.pojo.request.RefreshTokenRequest
import com.ampnet.userservice.controller.pojo.request.TokenRequest
import com.ampnet.userservice.controller.pojo.request.TokenRequestSocialInfo
import com.ampnet.userservice.controller.pojo.request.TokenRequestUserInfo
import com.ampnet.userservice.controller.pojo.response.AccessRefreshTokenResponse
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidLoginMethodException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.PasswordService
import com.ampnet.userservice.service.SocialService
import com.ampnet.userservice.service.TokenService
import com.ampnet.userservice.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class AuthenticationController(
    private val userService: UserService,
    private val socialService: SocialService,
    private val tokenService: TokenService,
    private val passwordService: PasswordService,
    private val objectMapper: ObjectMapper
) {

    companion object : KLogging()

    @PostMapping("/token")
    fun generateToken(@RequestBody @Valid tokenRequest: TokenRequest): ResponseEntity<AccessRefreshTokenResponse> {
        logger.debug { "Received request for token with: ${tokenRequest.loginMethod}" }
        val user: User = when (tokenRequest.loginMethod) {
            AuthMethod.EMAIL -> {
                val userInfo: TokenRequestUserInfo = objectMapper.convertValue(tokenRequest.credentials)
                val user = getUserByEmail(userInfo.email)
                validateEmailLogin(user, userInfo.password)
                user
            }
            AuthMethod.FACEBOOK -> {
                val userInfo: TokenRequestSocialInfo = objectMapper.convertValue(tokenRequest.credentials)
                val socialUser = socialService.getFacebookEmail(userInfo.token)
                val user = getUserByEmail(socialUser.email)
                validateSocialLogin(user, AuthMethod.FACEBOOK)
                user
            }
            AuthMethod.GOOGLE -> {
                val userInfo: TokenRequestSocialInfo = objectMapper.convertValue(tokenRequest.credentials)
                val socialUser = socialService.getGoogleEmail(userInfo.token)
                val user = getUserByEmail(socialUser.email)
                validateSocialLogin(user, AuthMethod.GOOGLE)
                user
            }
        }
        val accessAndRefreshToken = tokenService.generateAccessAndRefreshForUser(user)
        logger.debug { "User: ${user.uuid} successfully authenticated." }
        return ResponseEntity.ok(AccessRefreshTokenResponse(accessAndRefreshToken))
    }

    @PostMapping("/token/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AccessRefreshTokenResponse> {
        logger.debug { "Received request to refresh token" }
        return try {
            val accessAndRefreshToken = tokenService.generateAccessAndRefreshFromRefreshToken(request.refreshToken)
            ResponseEntity.ok(AccessRefreshTokenResponse(accessAndRefreshToken))
        } catch (ex: TokenException) {
            logger.info { ex.message }
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/forgot-password/token")
    fun generateForgotPasswordToken(@RequestBody @Valid request: MailCheckRequest): ResponseEntity<Unit> {
        logger.info { "Received request to generate forgot password token for email: ${request.email}" }
        val generated = passwordService.generateForgotPasswordToken(request.email)
        return if (generated) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/forgot-password")
    fun changePasswordWithToken(@RequestBody @Valid request: ChangePasswordTokenRequest): ResponseEntity<UserResponse> {
        logger.info { "Received request for forgot password, token = ${request.token}" }
        val user = passwordService.changePasswordWithToken(request.token, request.newPassword)
        return ResponseEntity.ok(UserResponse(user))
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to logout user: ${userPrincipal.uuid}" }
        tokenService.deleteRefreshToken(userPrincipal.uuid)
        return ResponseEntity.ok().build()
    }

    private fun validateEmailLogin(user: User, providedPassword: String) {
        val storedPasswordHash = user.password
        if (!passwordService.verifyPasswords(providedPassword, storedPasswordHash)) {
            logger.debug { "User passwords do not match" }
            throw BadCredentialsException("Wrong password!")
        }
    }

    private fun validateSocialLogin(user: User, authMethod: AuthMethod) {
        if (user.authMethod != authMethod) {
            throw InvalidLoginMethodException("Invalid login method. User: ${user.uuid} try to login with: $authMethod")
        }
    }

    private fun getUserByEmail(email: String): User = userService.find(email)
        ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "User with email: $email does not exists")
}
