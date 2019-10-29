package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.ChangePasswordRequest
import com.ampnet.userservice.controller.pojo.request.VerifyRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.service.UserService
import javax.validation.Valid
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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

    @PostMapping("/me/verify")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRO_PROFILE)")
    fun connectUserInfo(@RequestBody connectRequest: VerifyRequest): ResponseEntity<UserResponse> {
        val userUuid = ControllerUtils.getUserPrincipalFromSecurityContext().uuid
        logger.info {
            "Received request to connect user info to user: $userUuid, webSessionUuid: ${connectRequest.webSessionUuid}"
        }
        val user = userService.connectUserInfo(userUuid, connectRequest.webSessionUuid)
        return ResponseEntity.ok(UserResponse(user))
    }

    @PostMapping("/me/password")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRO_PROFILE)")
    fun changePassword(@RequestBody @Valid request: ChangePasswordRequest): ResponseEntity<UserResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to change password by user: ${userPrincipal.uuid}" }
        userService.find(userPrincipal.uuid)?.let {
            val user = userService.changePassword(it, request.oldPassword, request.newPassword)
            return ResponseEntity.ok(UserResponse(user))
        }
        logger.error("Non existing user: ${userPrincipal.uuid} trying to update password")
        return ResponseEntity.notFound().build()
    }
}
