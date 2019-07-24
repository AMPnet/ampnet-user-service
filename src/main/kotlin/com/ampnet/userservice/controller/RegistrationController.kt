package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.MailCheckRequest
import com.ampnet.userservice.controller.pojo.request.SignupRequest
import com.ampnet.userservice.controller.pojo.request.SignupRequestSocialInfo
import com.ampnet.userservice.controller.pojo.request.SignupRequestUserInfo
import com.ampnet.userservice.controller.pojo.response.MailCheckResponse
import com.ampnet.userservice.controller.pojo.response.MailResponse
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.service.IdentyumService
import com.ampnet.userservice.service.SocialService
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid
import javax.validation.Validator

@RestController
class RegistrationController(
    private val userService: UserService,
    private val socialService: SocialService,
    private val identyumService: IdentyumService,
    private val objectMapper: ObjectMapper,
    private val validator: Validator
) {
    companion object : KLogging()

    @PostMapping("/signup")
    fun createUser(@RequestBody request: SignupRequest): ResponseEntity<UserResponse> {
        logger.debug { "Received request to sign up with method: ${request.signupMethod}" }
        val createUserRequest = createUserRequest(request)
        validateRequestOrThrow(createUserRequest)
        val user = userService.createUser(createUserRequest)
        return ResponseEntity.ok(UserResponse(user))
    }

    @GetMapping("/mail-confirmation")
    fun mailConfirmation(@RequestParam("token") token: String): ResponseEntity<Void> {
        logger.debug { "Received to confirm mail with token: $token" }
        try {
            val tokenUuid = UUID.fromString(token)
            userService.confirmEmail(tokenUuid)?.let {
                logger.info { "Confirmed email for user: ${it.email}" }
                return ResponseEntity.ok().build()
            }
            logger.info { "User trying to confirm mail with non existing token: $tokenUuid" }
            return ResponseEntity.notFound().build()
        } catch (ex: IllegalArgumentException) {
            logger.warn { "User send token which is not UUID: $token" }
            throw InvalidRequestException(ErrorCode.REG_EMAIL_INVALID_TOKEN, "Token: $token is not in a valid format.")
        }
    }

    @GetMapping("/mail-confirmation/resend")
    fun resendMailConfirmation(): ResponseEntity<Any> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "User ${userPrincipal.email} requested to resend mail confirmation link" }
        userService.find(userPrincipal.email)?.let {
            userService.resendConfirmationMail(it)
            return ResponseEntity.ok().build()
        }
        logger.warn { "User ${userPrincipal.email} missing in database, trying to resend mail confirmation" }
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/mail-check")
    fun checkIfMailExists(@RequestBody @Valid request: MailCheckRequest): ResponseEntity<MailCheckResponse> {
        logger.debug { "Received request to check if email exists: $request" }
        val emailUsed = userService.find(request.email) != null
        return ResponseEntity.ok(MailCheckResponse(request.email, emailUsed))
    }

    @GetMapping("/mail-user-pending/{webSessionUuid}")
    fun getUserMailForIdentyumUuid(@PathVariable webSessionUuid: String): ResponseEntity<MailResponse> {
        logger.debug { "Received request to get email for webSessionUuid: $webSessionUuid" }
        identyumService.findUserInfo(webSessionUuid)?.let {
            return ResponseEntity.ok(MailResponse(it.verifiedEmail))
        }
        return ResponseEntity.notFound().build()
    }

    private fun createUserRequest(request: SignupRequest): CreateUserServiceRequest {
        try {
            val jsonString = objectMapper.writeValueAsString(request.userInfo)
            return when (request.signupMethod) {
                AuthMethod.EMAIL -> {
                    val userInfo: SignupRequestUserInfo = objectMapper.readValue(jsonString)
                    CreateUserServiceRequest(
                        request.webSessionUuid, userInfo.email, userInfo.password, request.signupMethod)
                }
                AuthMethod.GOOGLE -> {
                    val socialInfo: SignupRequestSocialInfo = objectMapper.readValue(jsonString)
                    val email = socialService.getGoogleEmail(socialInfo.token)
                    CreateUserServiceRequest(request.webSessionUuid, email, null, request.signupMethod)
                }
                AuthMethod.FACEBOOK -> {
                    val socialInfo: SignupRequestSocialInfo = objectMapper.readValue(jsonString)
                    val email = socialService.getFacebookEmail(socialInfo.token)
                    CreateUserServiceRequest(request.webSessionUuid, email, null, request.signupMethod)
                }
            }
        } catch (ex: MissingKotlinParameterException) {
            logger.info("Could not parse SignupRequest with method: ${request.signupMethod}")
            throw InvalidRequestException(
                ErrorCode.REG_INCOMPLETE, "Some fields missing or could not be parsed from JSON request.", ex)
        }
    }

    private fun validateRequestOrThrow(request: CreateUserServiceRequest) {
        val errors = validator.validate(request)
        if (errors.isNotEmpty()) {
            logger.info { "Invalid CreateUserServiceRequest: $request" }
            throw InvalidRequestException(ErrorCode.REG_INVALID, errors.joinToString("; ") { it.message })
        }
    }
}
