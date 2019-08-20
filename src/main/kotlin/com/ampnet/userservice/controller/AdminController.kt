package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.CreateAdminUserRequest
import com.ampnet.userservice.controller.pojo.request.RoleRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.controller.pojo.response.UsersListResponse
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.AdminService
import com.ampnet.userservice.service.UserService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
class AdminController(private val adminService: AdminService, private val userService: UserService) {

    companion object : KLogging()

    @PostMapping("/admin/user")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PWA_PROFILE)")
    fun createAdminUser(@RequestBody @Valid request: CreateAdminUserRequest): ResponseEntity<UserResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to create user with email: ${request.email} by admin: ${userPrincipal.uuid}" }
        val user = adminService.createAdminUser(request)
        return ResponseEntity.ok(UserResponse(user))
    }

    @GetMapping("/admin/user")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getUsers(): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to list all users" }
        val users = adminService.findAll()
        return generateUserListResponse(users)
    }

    @GetMapping("/admin/user/find")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun findByEmail(@RequestParam email: String): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to find user by email: $email" }
        val users = adminService.findByEmail(email)
        return generateUserListResponse(users)
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

    @GetMapping("/admin/user/admin")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getListOfAdminUsers(): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to get a list of admin users" }
        val users = adminService.findByRole(UserRoleType.ADMIN)
        return generateUserListResponse(users)
    }

    private fun generateUserListResponse(users: List<User>): ResponseEntity<UsersListResponse> {
        val usersResponse = users.map { UserResponse(it) }
        return ResponseEntity.ok(UsersListResponse(usersResponse))
    }
}
