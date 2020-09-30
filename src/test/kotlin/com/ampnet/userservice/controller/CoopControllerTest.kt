package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.response.CoopResponse
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.ZonedDateTime

class CoopControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var coopRepository: CoopRepository

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
            val request = CoopRequest(testContext.name)
            val result = mockMvc.perform(
                MockMvcRequestBuilders.post(coopPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val coopResponse: CoopResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(coopResponse.name).isEqualTo(testContext.name)
            assertThat(coopResponse.identifier).isEqualTo(testContext.identifier)
            assertThat(coopResponse.createdAt).isBefore(ZonedDateTime.now())
            assertThat(coopResponse.id).isNotNull()
        }
        verify("Coop is created") {
            val coop = coopRepository.findAll().first()
            assertThat(coop.name).isEqualTo(testContext.name)
            assertThat(coop.identifier).isEqualTo(testContext.identifier)
            assertThat(coop.createdAt).isBefore(ZonedDateTime.now())
            assertThat(coop.id).isNotNull()
        }
    }

    private class TestContext {
        lateinit var name: String
        lateinit var identifier: String
    }
}
