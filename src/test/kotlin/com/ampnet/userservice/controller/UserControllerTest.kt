package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.ChangePasswordRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.enums.PrivilegeType
import com.ampnet.userservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserControllerTest : ControllerTestBase() {

    private val pathMe = "/me"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "b2d05e1c-9348-40cc-a41e-4f6c06a80035", privileges = [PrivilegeType.PRO_PROFILE])
    fun mustBeAbleToGetOwnProfile() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            testContext.email = "test@test.com"
            testContext.uuid = UUID.fromString("b2d05e1c-9348-40cc-a41e-4f6c06a80035")
            createUser(testContext.email, uuid = testContext.uuid)
        }

        verify("The controller must return user data") {
            val result = mockMvc.perform(get(pathMe))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testContext.email)
            assertThat(userResponse.uuid).isEqualTo(testContext.uuid.toString())
        }
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "b2d05e1c-9348-40cc-a41e-4f6c06a80035", privileges = [PrivilegeType.PRO_PROFILE])
    fun mustThrowExceptionIfUserDoesNotExists() {
        suppose("User is not stored in database") {
            databaseCleanerService.deleteAllUsers()
            createUser("non-existing@user.com", uuid = UUID.randomUUID())
        }

        verify("Controller will throw exception for non existing user on /me path") {
            mockMvc.perform(get(pathMe))
                .andExpect(MockMvcResultMatchers.status().isNotFound)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRO_PROFILE])
    fun mustBeAbleToChangeOwnPassword() {
        suppose("User is stored in database") {
            databaseCleanerService.deleteAllUsers()
            testContext.oldPassword = "oldPassword"
            createUser(defaultEmail, password = testContext.oldPassword, uuid = defaultUuid)
        }

        verify("User can update password") {
            testContext.newPassword = "newPassword"
            val request = ChangePasswordRequest(testContext.oldPassword, testContext.newPassword)
            val result = mockMvc.perform(
                post("$pathMe/password")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.uuid).isEqualTo(defaultUuid.toString())
        }
        verify("User password is updated") {
            val optionalUser = userRepository.findById(defaultUuid)
            assertThat(optionalUser).isPresent
            assert(passwordEncoder.matches(testContext.newPassword, optionalUser.get().password))
        }
    }

    private class TestContext {
        var email = "john@smith.com"
        lateinit var uuid: UUID
        lateinit var oldPassword: String
        lateinit var newPassword: String
    }
}
