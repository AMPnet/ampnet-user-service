package com.ampnet.userservice.controller

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.response.RegisteredUsersResponse
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InternalException
import com.ampnet.userservice.service.CoopService
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.pojo.CoopServiceResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicController(
    private val userService: UserService,
    private val coopService: CoopService,
    private val applicationProperties: ApplicationProperties
) {

    @GetMapping("/public/user/count")
    fun countRegisteredUsers(@RequestParam(required = false) coop: String?): ResponseEntity<RegisteredUsersResponse> {
        val registeredUsers = userService.countAllUsers(coop)
        return ResponseEntity.ok(RegisteredUsersResponse(registeredUsers))
    }

    @GetMapping("/public/app/config/hostname/{hostname}")
    fun getAppConfigByHostname(@PathVariable hostname: String): ResponseEntity<CoopServiceResponse> {
        coopService.getCoopByHostname(hostname)?.let {
            return ResponseEntity.ok(it)
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/public/app/config/identifier/{identifier}")
    fun getAppConfigByIdentifier(@PathVariable identifier: String): ResponseEntity<CoopServiceResponse> {
        coopService.getCoopByIdentifier(identifier)?.let {
            return ResponseEntity.ok(it)
        }
        val defaultCoop = applicationProperties.coop.default
        coopService.getCoopByIdentifier(defaultCoop)?.let {
            return ResponseEntity.ok(it)
        }
        throw InternalException(ErrorCode.COOP_MISSING, "Missing default coop: $defaultCoop on platform")
    }
}
