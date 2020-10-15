package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.TestUserSignupRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TestControllerTest : ControllerTestBase() {

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToCreateTestUser() {
        suppose("User does not exists") {
            databaseCleanerService.deleteAllUsers()
        }

        verify("User can create test email account") {
            val request = TestUserSignupRequest(testContext.email, "password")
            val result = mockMvc.perform(
                MockMvcRequestBuilders.post("/test/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val user: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(user.email).isEqualTo(testContext.email)
            assertThat(user.firstName).isEqualTo(TestController.testName)
            assertThat(user.lastName).isEqualTo(TestController.testName)
        }
        verify("User is created") {
            val optionalUser = userRepository.findByEmail(testContext.email)
            assertThat(optionalUser).isPresent
            assertThat(optionalUser.get().userInfoId).isNull()
        }
    }

    private class TestContext {
        val email = "my@email.com"
    }
}
