package com.ampnet.userservice.controller

import com.ampnet.userservice.COOP
import com.ampnet.userservice.controller.pojo.response.RegisteredUsersResponse
import com.ampnet.userservice.persistence.model.Coop
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MvcResult
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
        suppose("There is one user in another coop") {
            createUser("another@coop.com", coop = "another-coop")
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
    fun mustBeAbleToGetCoopConfigByHostname() {
        suppose("There is coop") {
            databaseCleanerService.deleteAllCoop()
            testContext.coop = createCoop(COOP, testContext.config)
        }

        verify("User can get coop config by hostname") {
            val result = mockMvc.perform(
                get("$publicPath/app/config/hostname/${testContext.coop.hostname}")
            )
                .andExpect(status().isOk)
                .andReturn()
            verifyCoopResponse(result)
        }
    }

    @Test
    fun mustBeAbleToGetCoopConfigByIdentifier() {
        suppose("There is coop") {
            databaseCleanerService.deleteAllCoop()
            testContext.coop = createCoop(COOP, testContext.config)
        }

        verify("User can get coop config by identifier") {
            val result = mockMvc.perform(
                get("$publicPath/app/config/identifier/${testContext.coop.identifier}")
            )
                .andExpect(status().isOk)
                .andReturn()
            verifyCoopResponse(result)
        }
    }

    @Test
    fun mustBeAbleToGetDefaultCoopConfigForMissingIdentifier() {
        suppose("There is coop") {
            databaseCleanerService.deleteAllCoop()
            testContext.coop = createCoop(COOP, testContext.config)
        }

        verify("User will get default coop for non existing coop identifier") {
            val result = mockMvc.perform(
                get("$publicPath/app/config/identifier/missing")
            )
                .andExpect(status().isOk)
                .andReturn()
            verifyCoopResponse(result)
        }
    }

    @Test
    fun mustGetErrorForMissingDefaultCoop() {
        suppose("There is no default coop") {
            databaseCleanerService.deleteAllCoop()
        }

        verify("User will get default coop for non existing coop identifier") {
            mockMvc.perform(
                get("$publicPath/app/config/identifier/missing")
            )
                .andExpect(status().isInternalServerError)
        }
    }

    private fun verifyCoopResponse(result: MvcResult) {
        val coopResponse: CoopResponseTest = objectMapper.readValue(result.response.contentAsString)
        assertThat(coopResponse.name).isEqualTo(testContext.coop.name)
        assertThat(coopResponse.identifier).isEqualTo(testContext.coop.identifier)
        assertThat(coopResponse.hostname).isEqualTo(testContext.coop.hostname)
        assertThat(serializeConfig(coopResponse.config)).isEqualTo(testContext.config)
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
