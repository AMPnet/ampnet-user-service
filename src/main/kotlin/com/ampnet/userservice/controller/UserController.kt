package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.RoleRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.controller.pojo.response.UsersListResponse
import com.ampnet.userservice.service.UserService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserController(private val userService: UserService) {

    companion object : KLogging()

    @GetMapping("/me")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRO_PROFILE)")
    fun me(): ResponseEntity<UserResponse> {
        logger.debug { "Received request for my profile" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        userService.find(userPrincipal.email)?.let {
            return ResponseEntity.ok(UserResponse(it))
        }

        logger.error("Non existing user: ${userPrincipal.email} trying to get his profile")
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getUsers(): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to list all users" }
        val users = userService.findAll().map { UserResponse(it) }
        return ResponseEntity.ok(UsersListResponse(users))
    }

    @GetMapping("/users/{uuid}")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getUser(@PathVariable("uuid") uuid: UUID): ResponseEntity<UserResponse> {
        logger.debug { "Received request for user info with uuid: $uuid" }
        return userService.find(uuid)?.let { ResponseEntity.ok(UserResponse(it)) }
                ?: ResponseEntity.notFound().build()
    }

    @PostMapping("/users/{uuid}/role")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PWA_PROFILE)")
    fun changeUserRole(
        @PathVariable("uuid") uuid: UUID,
        @RequestBody request: RoleRequest
    ): ResponseEntity<UserResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug {
            "Received request by user: ${userPrincipal.email} to change user: $uuid role to ${request.role}"
        }
        val user = userService.changeUserRole(uuid, request.role)
        return ResponseEntity.ok(UserResponse(user))
    }
}
