package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.ChangePasswordRequest
import com.ampnet.userservice.controller.pojo.request.UserUpdateRequest
import com.ampnet.userservice.service.PasswordService
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.pojo.UserResponse
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
class UserController(private val userService: UserService, private val passwordService: PasswordService) {

    companion object : KLogging()

    @GetMapping("/me")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRO_PROFILE)")
    fun me(): ResponseEntity<UserResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request for my profile by user: ${userPrincipal.uuid}" }

        userService.find(userPrincipal.uuid)?.let {
            return ResponseEntity.ok(it)
        }
        logger.error("Non existing user: ${userPrincipal.uuid} trying to get his profile")
        return ResponseEntity.notFound().build()
    }

    @PostMapping("/me/password")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRO_PROFILE)")
    fun changePassword(@RequestBody @Valid request: ChangePasswordRequest): ResponseEntity<UserResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to change password by user: ${userPrincipal.uuid}" }
        val user = passwordService.changePassword(userPrincipal.uuid, request.oldPassword, request.newPassword)
        return ResponseEntity.ok(user)
    }

    @PutMapping("/me/update")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PWO_PROFILE)")
    fun updateUser(@RequestBody @Valid request: UserUpdateRequest): ResponseEntity<UserResponse> {
        val userUuid = ControllerUtils.getUserPrincipalFromSecurityContext().uuid
        logger.debug { "Received request to update user: $userUuid" }
        val user = userService.update(userUuid, request)
        return ResponseEntity.ok(user)
    }
}
