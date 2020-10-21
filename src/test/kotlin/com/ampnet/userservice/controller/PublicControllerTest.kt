package com.ampnet.userservice.controller

import com.ampnet.userservice.COOP
import com.ampnet.userservice.controller.pojo.response.RegisteredUsersResponse
import com.ampnet.userservice.persistence.model.Coop
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PublicControllerTest : ControllerTestBase() {

    private val publicPath = "/public"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
    }

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

    @Test
    fun mustBeAbleToGetCoopConfig() {
        suppose("There is coop") {
            databaseCleanerService.deleteAllCoop()
            testContext.coop = createCoop(COOP, testContext.config)
        }

        verify("User can get coop config") {
            val result = mockMvc.perform(
                get("$publicPath/app/config/${testContext.coop.host}")
            )
                .andExpect(status().isOk)
                .andReturn()

            assertThat(result.response.contentAsString).contains(testContext.config)
        }
    }

    private class TestContext {
        lateinit var coop: Coop
        val config =
            """
                {
                    "public": true,
                    "show_version":false
                }
            """.replace("\\s".toRegex(), "")
    }
}
