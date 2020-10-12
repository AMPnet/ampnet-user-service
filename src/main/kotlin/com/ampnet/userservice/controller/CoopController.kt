package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.response.CoopResponse
import com.ampnet.userservice.service.CoopService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class CoopController(private val coopService: CoopService) {

    companion object : KLogging()

    @PostMapping("/coop")
    fun createCoop(@Valid @RequestBody request: CoopRequest): ResponseEntity<CoopResponse> {
        logger.debug { "Received request to create coop: $request" }
        val coop = coopService.createCoop(request)
        return ResponseEntity.ok(CoopResponse(coop))
    }
}
