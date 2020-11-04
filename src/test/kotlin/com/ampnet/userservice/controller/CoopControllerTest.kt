package com.ampnet.userservice.controller

import com.ampnet.userservice.COOP
import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.security.WithMockCrowdfundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class CoopControllerTest : ControllerTestBase() {

    private val coopPath = "/coop"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllCoop()
        testContext = TestContext()
    }

    @Test
    fun mustCreateCoop() {
        verify("User can create coop") {
            testContext.name = "New Coop a"
            testContext.identifier = "new-coop-a"
            val configMap: Map<String, Any> = objectMapper.readValue(testContext.config)
            val request = CoopRequest(testContext.identifier, testContext.name, testContext.hostname, configMap)
            val result = mockMvc.perform(
                post(coopPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val coopResponse: CoopResponseTest = objectMapper.readValue(result.response.contentAsString)
            assertThat(coopResponse.identifier).isEqualTo(testContext.identifier)
            assertThat(coopResponse.name).isEqualTo(testContext.name)
            assertThat(coopResponse.createdAt).isBefore(ZonedDateTime.now())
            assertThat(coopResponse.hostname).isEqualTo(testContext.hostname)
            assertThat(serializeConfig(coopResponse.config)).isEqualTo(testContext.config)
        }
        verify("Coop is created") {
            val coop = coopRepository.findAll().first()
            assertThat(coop.name).isEqualTo(testContext.name)
            assertThat(coop.identifier).isEqualTo(testContext.identifier)
            assertThat(coop.createdAt).isBefore(ZonedDateTime.now())
            assertThat(coop.hostname).isEqualTo(testContext.hostname)
            assertThat(coop.config).isEqualTo(testContext.config)
        }
    }

    @Test
    @WithMockCrowdfundUser(role = UserRole.ADMIN, coop = COOP)
    fun mustBeAbleToUpdateCoop() {
        suppose("There is coop") {
            testContext.coop = createCoop(COOP)
        }

        verify("Admin can update coop") {
            testContext.hostname = "new.my.host"
            testContext.name = "New name"
            val configMap: Map<String, Any> = objectMapper.readValue(testContext.config)
            val request = CoopUpdateRequest(testContext.name, testContext.hostname, configMap)
            val result = mockMvc.perform(
                put(coopPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val coopResponse: CoopResponseTest = objectMapper.readValue(result.response.contentAsString)
            assertThat(coopResponse.name).isEqualTo(testContext.name)
            assertThat(coopResponse.identifier).isEqualTo(COOP)
            assertThat(coopResponse.hostname).isEqualTo(testContext.hostname)
            assertThat(serializeConfig(coopResponse.config)).isEqualTo(testContext.config)
        }
    }

    @Test
    @WithMockCrowdfundUser(role = UserRole.ADMIN, coop = COOP)
    fun mustBeAbleToGetMyCoop() {
        suppose("There is coop") {
            testContext.coop = createCoop(COOP, config = testContext.config)
        }

        verify("Admin can get his coop") {
            val result = mockMvc.perform(
                get(coopPath)
            )
                .andExpect(status().isOk)
                .andReturn()

            val coopResponse: CoopResponseTest = objectMapper.readValue(result.response.contentAsString)
            assertThat(coopResponse.name).isEqualTo(testContext.coop.name)
            assertThat(coopResponse.identifier).isEqualTo(testContext.coop.identifier)
            assertThat(coopResponse.hostname).isEqualTo(testContext.coop.hostname)
            assertThat(serializeConfig(coopResponse.config)).isEqualTo(testContext.config)
        }
    }

    private fun serializeConfig(config: Map<String, Any>?) = objectMapper.writeValueAsString(config)

    private class TestContext {
        lateinit var name: String
        lateinit var identifier: String
        lateinit var coop: Coop
        var hostname = "ampnet.io"
        var config =
            """
                {
                    "colors": {
                        "main": "black"
                    },
                    "arkane": "PRODUCTION",
                    "test": true,
                    "retry": 2
                }
            """.replace("\\s".toRegex(), "")
    }

    private data class CoopResponseTest(
        val identifier: String,
        val name: String,
        val createdAt: ZonedDateTime,
        val hostname: String,
        val config: Map<String, Any>?
    )
}
