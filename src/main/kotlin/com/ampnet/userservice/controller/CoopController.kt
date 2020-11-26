package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.service.CoopService
import com.ampnet.userservice.service.pojo.CoopServiceResponse
import mu.KLogging
import org.springframework.cache.annotation.CacheEvict
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
    @CacheEvict(value = [COOP_CACHE], allEntries = true)
    fun createCoop(@Valid @RequestBody request: CoopRequest): ResponseEntity<CoopServiceResponse> {
        logger.info { "Received request to create coop: $request" }
        val coop = coopService.createCoop(request)
        return ResponseEntity.ok(coop)
    }

    @PutMapping("/coop")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PWA_COOP)")
    @CacheEvict(value = [COOP_CACHE], allEntries = true)
    fun updateCoop(@Valid @RequestBody request: CoopUpdateRequest): ResponseEntity<CoopServiceResponse> {
        logger.info { "Received request to update coop: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        coopService.updateCoop(userPrincipal.coop, request)?.let {
            return ResponseEntity.ok(it)
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/coop")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_COOP)")
    fun getMyCoop(): ResponseEntity<CoopServiceResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        coopService.getCoopByIdentifier(userPrincipal.coop)?.let {
            return ResponseEntity.ok(it)
        }
        return ResponseEntity.notFound().build()
    }
}
