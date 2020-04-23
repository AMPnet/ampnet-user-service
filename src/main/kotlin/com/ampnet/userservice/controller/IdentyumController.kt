package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.IdentyumPayloadRequest
import com.ampnet.userservice.exception.IdentyumException
import com.ampnet.userservice.service.IdentyumService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
    fun postUserData(@RequestBody request: IdentyumPayloadRequest): ResponseEntity<String> {
        logger.info { "Received Identyum payload: $request" }
        return try {
            val userInfo = identyumService.createUserInfo(request)
            logger.info { "Successfully stored Identyum user - webSessionUuid: ${userInfo.webSessionUuid}" }
            ResponseEntity.ok().build()
        } catch (ex: IdentyumException) {
            logger.error("Could not store UserInfo from Identyum request", ex)
            ResponseEntity.unprocessableEntity().body("Error: ${ex.message}\n Cause: ${ex.cause?.message}")
        }
    }
}
