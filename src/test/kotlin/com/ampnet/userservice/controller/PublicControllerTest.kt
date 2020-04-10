package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.response.RegisteredUsersResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PublicControllerTest : ControllerTestBase() {

    private val publicPath = "/public"

    @Test
    fun mustBeAbleToCountAllUsers() {
        suppose("There are 2 users") {
            databaseCleanerService.deleteAllUsers()
            createUser("first@email.com")
            createUser("second@email.com")
        }

        verify("User can get number of users on platform") {
            val result = mockMvc.perform(get("$publicPath/user/count"))
                .andExpect(status().isOk)
                .andReturn()

            val response: RegisteredUsersResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.registered).isEqualTo(2)
        }
    }
}
