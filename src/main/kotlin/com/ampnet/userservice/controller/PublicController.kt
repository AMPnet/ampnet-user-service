package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.response.ConfigResponse
import com.ampnet.userservice.controller.pojo.response.RegisteredUsersResponse
import com.ampnet.userservice.service.AdminService
import com.ampnet.userservice.service.CoopService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicController(private val adminService: AdminService, private val coopService: CoopService) {

    @GetMapping("/public/user/count")
    fun countRegisteredUsers(): ResponseEntity<RegisteredUsersResponse> {
        val registeredUsers = adminService.countAllUsers()
        return ResponseEntity.ok(RegisteredUsersResponse(registeredUsers))
    }

    @GetMapping("/public/app/config/{host}")
    fun getAppConfig(@PathVariable host: String): ResponseEntity<ConfigResponse> {
        coopService.getCoopByHost(host)?.let {
            return ResponseEntity.ok(ConfigResponse(it.config))
        }
        return ResponseEntity.notFound().build()
    }
}
