package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.VerifyRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.exception.IdentyumException
import com.ampnet.userservice.service.IdentyumService
import com.ampnet.userservice.service.UserService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class IdentyumController(private val identyumService: IdentyumService, private val userService: UserService) {

    companion object : KLogging()

    @GetMapping("/identyum/token")
    fun getIdentyumToken(): ResponseEntity<String> {
        logger.debug { "Received request to get Identyum token" }
        val identyumResponse = identyumService.getToken()
        return ResponseEntity.ok(identyumResponse)
    }

    @PostMapping("/identyum")
    fun postUserData(
        @RequestBody request: String,
        @RequestHeader("secret-key") secretKey: String,
        @RequestHeader("signature") signature: String
    ): ResponseEntity<String> {
        // this route can be used for signing document
        logger.info { "Received Identyum data" }
        return try {
            val userInfo = identyumService.createUserInfo(request, secretKey, signature)
            logger.info { "Successfully stored Identyum user - ClientSessionUuid: ${userInfo.sessionId}" }
            ResponseEntity.ok().build()
        } catch (ex: IdentyumException) {
            logger.error("Could not store UserInfo from Identyum request", ex)
            ResponseEntity.unprocessableEntity().body("Error: ${ex.message}\n Cause: ${ex.cause?.message}")
        }
    }

    @PostMapping("/identyum/verify")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRO_PROFILE)")
    fun connectUserInfo(@RequestBody connectRequest: VerifyRequest): ResponseEntity<UserResponse> {
        val userUuid = ControllerUtils.getUserPrincipalFromSecurityContext().uuid
        logger.info {
            "Received request to connect user info to user: $userUuid, " +
                "sessionState(clientSessionUuid): ${connectRequest.sessionState}"
        }
        val user = userService.connectUserInfo(userUuid, connectRequest.sessionState)
        return ResponseEntity.ok(UserResponse(user))
    }
}
