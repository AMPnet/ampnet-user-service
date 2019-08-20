package com.ampnet.userservice.controller

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.request.TestUserSignupRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime
import java.util.UUID
import javax.validation.Valid

@RestController
class TestController(
    private val userService: UserService,
    private val userInfoRepository: UserInfoRepository,
    private val applicationProperties: ApplicationProperties
) {

    @PostMapping("/test/signup")
    fun createUser(@RequestBody @Valid request: TestUserSignupRequest): ResponseEntity<UserResponse> {
        if (applicationProperties.testUser.enabled.not()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val webSessionUuid = UUID.randomUUID().toString()
        val createUserInfo = createTestUserInfo(webSessionUuid)
        val createUserRequest = CreateUserServiceRequest(
            createUserInfo.webSessionUuid, request.email, request.password, AuthMethod.EMAIL)
        val user = userService.createUser(createUserRequest)
        return ResponseEntity.ok(UserResponse(user))
    }

    private fun createTestUserInfo(webSessionUuid: String): UserInfo {
        val userInfo = UserInfo(0,
            webSessionUuid,
            "Test",
            "Test",
            "test@test.com",
            "+000",
            "TST",
            "00-00-0000",
            "0000-0000-0000-0000",
            "ID",
            "000000",
            "HRV",
            true,
            "city",
            "county",
            "street",
            ZonedDateTime.now(),
            false
        )
        return userInfoRepository.save(userInfo)
    }
}
