package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.response.RegisteredUsersResponse
import com.ampnet.userservice.service.AdminService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicController(private val adminService: AdminService) {

    @GetMapping("/public/user/count")
    fun countRegisteredUsers(): ResponseEntity<RegisteredUsersResponse> {
        val registeredUsers = adminService.countAllUsers()
        return ResponseEntity.ok(RegisteredUsersResponse(registeredUsers))
    }
}
