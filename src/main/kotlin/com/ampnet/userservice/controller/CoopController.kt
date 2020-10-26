package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.controller.pojo.response.CoopResponse
import com.ampnet.userservice.service.CoopService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class CoopController(private val coopService: CoopService) {

    companion object : KLogging()

    @PostMapping("/coop")
    fun createCoop(@Valid @RequestBody request: CoopRequest): ResponseEntity<CoopResponse> {
        logger.info { "Received request to create coop: $request" }
        val coop = coopService.createCoop(request)
        return ResponseEntity.ok(CoopResponse(coop))
    }

    @PutMapping("/coop")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PWA_COOP)")
    fun updateCoop(@Valid @RequestBody request: CoopUpdateRequest): ResponseEntity<CoopResponse> {
        logger.info { "Received request to update coop: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        coopService.updateCoop(userPrincipal.coop, request)?.let {
            return ResponseEntity.ok(CoopResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/coop")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_COOP)")
    fun getMyCoop(): ResponseEntity<CoopResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        coopService.getCoopByIdentifier(userPrincipal.coop)?.let {
            return ResponseEntity.ok(CoopResponse(it))
        }
        return ResponseEntity.notFound().build()
    }
}
