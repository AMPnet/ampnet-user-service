package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.service.CoopService
import com.ampnet.userservice.service.ReCaptchaService
import com.ampnet.userservice.service.pojo.CoopServiceResponse
import mu.KLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import javax.validation.Valid

@RestController
class CoopController(
    private val coopService: CoopService,
    private val reCaptchaService: ReCaptchaService
) {
    companion object : KLogging()

    @PostMapping("/coop", consumes = ["multipart/form-data"])
    @CacheEvict(value = [COOP_CACHE], allEntries = true)
    fun createCoop(
        @Valid @RequestPart("request") request: CoopRequest,
        @RequestParam("logo", required = true) logo: MultipartFile?
    ): ResponseEntity<CoopServiceResponse> {
        logger.info { "Received request to create coop: $request" }
        reCaptchaService.validateResponseToken(request.reCaptchaToken)
        val coop = coopService.createCoop(request, logo)
        return ResponseEntity.ok(coop)
    }

    @PutMapping("/coop")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PWA_COOP)")
    @CacheEvict(value = [COOP_CACHE], allEntries = true)
    fun updateCoop(
        @Valid @RequestPart("request") request: CoopUpdateRequest,
        @RequestParam("logo", required = true) logo: MultipartFile?
    ): ResponseEntity<CoopServiceResponse> {
        logger.info { "Received request to update coop: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        coopService.updateCoop(userPrincipal.coop, request, logo)?.let {
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
