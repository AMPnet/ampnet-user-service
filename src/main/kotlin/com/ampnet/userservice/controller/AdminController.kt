package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.controller.pojo.response.UsersListResponse
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.AdminService
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.pojo.UserCount
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AdminController(private val adminService: AdminService, private val userService: UserService) {

    companion object : KLogging()

    @GetMapping("/admin/user")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getUsers(pageable: Pageable): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to list all users" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val users = adminService.findAll(userPrincipal.coop, pageable)
        return generateUserListResponse(users)
    }

    @GetMapping("/admin/user/find")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun findByEmail(@RequestParam email: String, pageable: Pageable): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to find user by email: $email" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val users = adminService.findByEmail(userPrincipal.coop, email, pageable)
        return generateUserListResponse(users)
    }

    @GetMapping("/admin/user/{uuid}")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getUser(@PathVariable("uuid") uuid: UUID): ResponseEntity<UserResponse> {
        logger.debug { "Received request for user info with uuid: $uuid" }
        return userService.find(uuid)?.let { ResponseEntity.ok(UserResponse(it)) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/admin/user/admin")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getListOfAdminUsers(pageable: Pageable): ResponseEntity<UsersListResponse> {
        logger.debug { "Received request to get a list of admin users" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val users = adminService.findByRole(userPrincipal.coop, UserRole.ADMIN, pageable)
        return generateUserListResponse(users)
    }

    @GetMapping("/admin/user/count")
    @PreAuthorize("hasAuthority(T(com.ampnet.userservice.enums.PrivilegeType).PRA_PROFILE)")
    fun getUserCount(): ResponseEntity<UserCount> {
        logger.debug { "Received request to get user count" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val userCount = adminService.countUsers(userPrincipal.coop)
        return ResponseEntity.ok(userCount)
    }

    private fun generateUserListResponse(users: Page<User>): ResponseEntity<UsersListResponse> {
        val usersResponse = users.map { UserResponse(it) }.toList()
        return ResponseEntity.ok(UsersListResponse(usersResponse, users.number, users.totalPages))
    }
}
