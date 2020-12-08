package com.ampnet.userservice.controller

import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.service.VeriffService
import com.ampnet.userservice.service.pojo.ServiceVerificationResponse
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class VeriffController(private val veriffService: VeriffService) {

    companion object : KLogging()

    @GetMapping("/veriff/session")
    fun getVeriffSession(): ResponseEntity<ServiceVerificationResponse> {
        val user = ControllerUtils.getUserPrincipalFromSecurityContext().uuid
        logger.info { "Received request to get veriff session for user: $user" }
        return try {
            veriffService.getVeriffSession(user)?.let {
                return ResponseEntity.ok(it)
            }
            logger.warn("Could not get veriff session")
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
        } catch (ex: VeriffException) {
            logger.warn("Could not get veriff session", ex)
            ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
        }
    }

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
            if (userInfo == null) {
                logger.info { "Veriff profile not approved. Veriff data: $data" }
            } else {
                logger.info { "Successfully verified Veriff session: ${userInfo.sessionId}" }
            }
            ResponseEntity.ok().build()
        } catch (ex: VeriffException) {
            logger.warn("Failed to complete Veriff flow.", ex)
            logger.info { "Veriff failed data: $data" }
            ResponseEntity.badRequest().build()
        }
    }
}
