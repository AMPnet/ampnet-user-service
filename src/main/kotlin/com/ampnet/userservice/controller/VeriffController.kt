package com.ampnet.userservice.controller

import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.service.VeriffService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class VeriffController(private val veriffService: VeriffService) {

    companion object : KLogging()

    @PostMapping("/veriff/webhook")
    fun sendUserVerificationData(
        @RequestBody data: String,
        @RequestHeader("X-AUTH-CLIENT") client: String,
        @RequestHeader("X-SIGNATURE") signature: String
    ): ResponseEntity<Unit> {
        logger.info { "Received Veriff data" }
        return try {
            veriffService.verifyClient(client)
            veriffService.verifySignature(signature, data)
            val userInfo = veriffService.saveUserVerificationData(data)
            logger.info { "Successfully verified Veriff session: ${userInfo.sessionId}" }
            ResponseEntity.ok().build()
        } catch (ex: VeriffException) {
            logger.warn("Failed to complete Veriff flow.", ex)
            logger.info { "Veriff failed data: $data" }
            ResponseEntity.badRequest().build()
        }
    }
}
