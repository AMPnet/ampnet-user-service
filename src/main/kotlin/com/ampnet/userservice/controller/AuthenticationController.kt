package com.ampnet.userservice.controller

import com.ampnet.userservice.config.auth.TokenProvider
import com.ampnet.userservice.config.auth.UserPrincipal
import com.ampnet.userservice.controller.pojo.request.RefreshTokenRequest
import com.ampnet.userservice.controller.pojo.request.TokenRequest
import com.ampnet.userservice.controller.pojo.request.TokenRequestSocialInfo
import com.ampnet.userservice.controller.pojo.request.TokenRequestUserInfo
import com.ampnet.userservice.controller.pojo.response.AuthTokenResponse
import com.ampnet.userservice.exception.InvalidLoginMethodException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.TokenException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.SocialService
import com.ampnet.userservice.service.RefreshTokenService
import com.ampnet.userservice.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthenticationController(
    private val jwtTokenProvider: TokenProvider,
    private val userService: UserService,
    private val socialService: SocialService,
    private val refreshTokenService: RefreshTokenService,
    private val objectMapper: ObjectMapper,
    private val passwordEncoder: PasswordEncoder
) {

    companion object : KLogging()

    @PostMapping("/token")
    fun generateToken(@RequestBody tokenRequest: TokenRequest): ResponseEntity<AuthTokenResponse> {
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
                val email = socialService.getFacebookEmail(userInfo.token)
                val user = getUserByEmail(email)
                validateSocialLogin(user, AuthMethod.FACEBOOK)
                user
            }
            AuthMethod.GOOGLE -> {
                val userInfo: TokenRequestSocialInfo = objectMapper.convertValue(tokenRequest.credentials)
                val email = socialService.getGoogleEmail(userInfo.token)
                val user = getUserByEmail(email)
                validateSocialLogin(user, AuthMethod.GOOGLE)
                user
            }
        }

        val token = jwtTokenProvider.generateToken(UserPrincipal(user))
        val refreshToken = refreshTokenService.generateRefreshToken(user)
        logger.debug { "User: ${user.uuid} successfully authenticated." }

        // TODO: change response to return with refresh token and expiration time
        return ResponseEntity.ok(AuthTokenResponse(token))
    }

    @PostMapping("/token/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AuthTokenResponse> {
        logger.debug { "Received request to refresh token" }
        return try {
            val user = refreshTokenService.getUserForToken(request.refreshToken)
            val newAccessToken = jwtTokenProvider.generateToken(UserPrincipal(user))
            ResponseEntity.ok(AuthTokenResponse(newAccessToken))
        } catch (ex: TokenException) {
            logger.info { ex.message }
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to logout user: ${userPrincipal.uuid}" }
        refreshTokenService.deleteRefreshToken(userPrincipal.uuid)
        return ResponseEntity.ok().build()
    }

    private fun validateEmailLogin(user: User, providedPassword: String) {
        val storedPasswordHash = user.password
        if (!passwordEncoder.matches(providedPassword, storedPasswordHash)) {
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
