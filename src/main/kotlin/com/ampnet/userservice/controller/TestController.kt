package com.ampnet.userservice.controller

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.request.CreateAdminUserRequest
import com.ampnet.userservice.controller.pojo.request.TestUserSignupRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.service.AdminService
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class TestController(
    private val adminService: AdminService,
    private val applicationProperties: ApplicationProperties
) {

    companion object {
        private val logger = KotlinLogging.logger {}
        internal const val testName = "TEST"
    }

    @PostMapping("/test/signup")
    fun createUser(@RequestBody @Valid request: TestUserSignupRequest): ResponseEntity<UserResponse> {
        logger.info { "Received request to create test user" }
        if (applicationProperties.user.creatingTestUser.not()) {
            logger.info { "Creating test user is disabled in application properties" }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val testUserRequest = CreateAdminUserRequest(
            request.email, testName, testName, request.password, UserRoleType.USER
        )
        val user = adminService.createUser(testUserRequest)
        logger.info { "Created test user: ${user.email} - ${user.uuid}" }
        return ResponseEntity.ok(UserResponse(user))
    }
}
