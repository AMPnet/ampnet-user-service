package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.service.UserService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(private val userService: UserService) {

    companion object : KLogging()

    @GetMapping("/me")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRO_PROFILE)")
    fun me(): ResponseEntity<UserResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request for my profile by user: ${userPrincipal.uuid}" }

        userService.find(userPrincipal.uuid)?.let {
            return ResponseEntity.ok(UserResponse(it))
        }
        logger.error("Non existing user: ${userPrincipal.uuid} trying to get his profile")
        return ResponseEntity.notFound().build()
    }
}
