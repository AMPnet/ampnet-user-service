package com.ampnet.userservice.controller

import com.ampnet.userservice.exception.IdentyumException
import com.ampnet.userservice.service.IdentyumService
import com.ampnet.userservice.service.pojo.IdentyumTokenServiceResponse
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class IdentyumController(private val identyumService: IdentyumService) {

    companion object : KLogging()

    @GetMapping("/identyum/token")
    fun getIdentyumToken(): ResponseEntity<IdentyumTokenServiceResponse> {
        logger.debug { "Received request to get Identyum token" }
        val user = ControllerUtils.getUserPrincipalFromSecurityContext().uuid
        val identyumResponse = identyumService.getToken(user)
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
            identyumService.createUserInfo(request, secretKey, signature)?.let {
                logger.info { "Successfully stored Identyum user - ClientSessionUuid: ${it.sessionId}" }
            }
            ResponseEntity.ok().build()
        } catch (ex: IdentyumException) {
            logger.error("Could not store UserInfo from Identyum request", ex)
            ResponseEntity.unprocessableEntity().body("Error: ${ex.message}\n Cause: ${ex.cause?.message}")
        }
    }
}
