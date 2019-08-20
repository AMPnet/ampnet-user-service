package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.CreateAdminUserRequest
import com.ampnet.userservice.controller.pojo.request.RoleRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.controller.pojo.response.UsersListResponse
import com.ampnet.userservice.service.AdminService
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
class AdminController(private val adminService: AdminService, private val userService: UserService) {

    companion object : KLogging()

    @PostMapping("/admin/user")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PWA_PROFILE)")
    fun createAdminUser(@RequestBody request: CreateAdminUserRequest): ResponseEntity<UserResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to create user with email: ${request.email} by admin: ${userPrincipal.uuid}" }
        return ResponseEntity.ok().build()
    }

    @GetMapping("/admin/user")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getUsers(): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to list all users" }
        val users = adminService.findAll().map { UserResponse(it) }
        return ResponseEntity.ok(UsersListResponse(users))
    }

    @GetMapping("/admin/user/email")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun findByEmail(@PathVariable email: String): ResponseEntity<UsersListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to find user by email: $email" }

        // TODO: implement
        return ResponseEntity.ok().build()
    }

    @GetMapping("/admin/user/{uuid}")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getUser(@PathVariable("uuid") uuid: UUID): ResponseEntity<UserResponse> {
        logger.debug { "Received request for user info with uuid: $uuid" }
        return userService.find(uuid)?.let {
            ResponseEntity.ok(UserResponse(it))
        } ?: ResponseEntity.notFound().build()
    }

    @PostMapping("/admin/user/{uuid}/role")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PWA_PROFILE)")
    fun changeUserRole(
        @PathVariable("uuid") uuid: UUID,
        @RequestBody request: RoleRequest
    ): ResponseEntity<UserResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info {
            "Received request by user: ${userPrincipal.email} to change user: $uuid role to ${request.role}"
        }
        val user = adminService.changeUserRole(uuid, request.role)
        return ResponseEntity.ok(UserResponse(user))
    }

    @GetMapping("/admin/list/admin")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getListOfAdminUsers(): ResponseEntity<UsersListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get a list of admin users" }
        // TODO: implement
        return ResponseEntity.ok().build()
    }

    @GetMapping("/admin/list/moderator")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getListOfModeratorUsers(): ResponseEntity<UsersListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get a list of moderator users" }

        // TODO: implement
        return ResponseEntity.ok().build()
    }
}
