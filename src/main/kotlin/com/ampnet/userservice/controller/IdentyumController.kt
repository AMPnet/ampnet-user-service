package com.ampnet.userservice.controller

import com.ampnet.userservice.exception.IdentyumException
import com.ampnet.userservice.service.IdentyumService
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
        logger.info { "Received Identyum data" }
        return try {
            val userInfo = identyumService.createUserInfo(request, secretKey, signature)
            logger.info { "Successfully stored Identyum user - ClientSessionUuid: ${userInfo.clientSessionUuid}" }
            ResponseEntity.ok().build()
        } catch (ex: IdentyumException) {
            logger.error("Could not store UserInfo from Identyum request", ex)
            ResponseEntity.unprocessableEntity().body("Error: ${ex.message}\n Cause: ${ex.cause?.message}")
        }
    }
}
