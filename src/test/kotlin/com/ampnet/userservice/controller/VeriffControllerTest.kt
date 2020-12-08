package com.ampnet.userservice.controller

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.VeriffSession
import com.ampnet.userservice.persistence.repository.VeriffSessionRepository
import com.ampnet.userservice.security.WithMockCrowdfundUser
import com.ampnet.userservice.service.pojo.ServiceVerificationResponse
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.client.RestTemplate
import java.time.ZonedDateTime
import java.util.UUID

class VeriffControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var veriffSessionRepository: VeriffSessionRepository

    private val veriffPath = "/veriff"
    private val xClientHeader = "X-AUTH-CLIENT"
    private val xSignature = "X-SIGNATURE"

    private lateinit var testContext: TestContext
    private lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun init() {
        testContext = TestContext()
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun mustStoreUserInfoFromVeriff() {
        suppose("User has no user info") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllUserInfos()
            testContext.user =
                createUser("veriff@email.com", uuid = UUID.fromString("5750f893-29fa-4910-8304-62f834338f47"))
        }

        verify("Controller will accept valid data") {
            val request = getResourceAsText("/veriff/response-with-vendor-data.json")
            mockMvc.perform(
                post("$veriffPath/webhook")
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(xClientHeader, applicationProperties.veriff.apiKey)
                    .header(xSignature, "e73fe0d8b416861d42c6839ec126e7bc7b020c1c19ff7df93dfc96b66e81b5c8")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }
        verify("User info is stored") {
            val userInfo = userInfoRepository.findBySessionId("12df6045-3846-3e45-946a-14fa6136d78b")
            assertThat(userInfo).isPresent
            assertThat(userInfo.get().connected).isTrue()
        }
    }

    @Test
    @WithMockCrowdfundUser(uuid = "4c2c2950-7a20-4fd7-b37f-f1d63a8211b4")
    fun mustReturnVeriffSession() {
        suppose("User has an account") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllUserInfos()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user =
                createUser("resubmission@email.com", uuid = UUID.fromString("4c2c2950-7a20-4fd7-b37f-f1d63a8211b4"))
            val veriffSession = VeriffSession(
                "44927492-8799-406e-8076-933bc9164ebc",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now()
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff will return decision response") {
            val response = getResourceAsText("/veriff/response-declined.json")
            mockVeriffResponse(response, HttpMethod.GET, "/v1/sessions/${testContext.veriffSession.id}/decision")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Controller will return new veriff session") {
            val result = mockMvc.perform(get("$veriffPath/session"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            val veriffResponse: ServiceVerificationResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(veriffResponse.decision).isNotNull
            assertThat(veriffResponse.verificationUrl).isEqualTo("https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new-url")
        }
    }

    private fun mockVeriffResponse(body: String, method: HttpMethod, path: String) {
        val status = MockRestResponseCreators.withStatus(HttpStatus.OK)
        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(applicationProperties.veriff.baseUrl + path)
        )
            .andExpect(MockRestRequestMatchers.method(method))
            .andRespond(status.body(body).contentType(MediaType.APPLICATION_JSON))
    }

    private class TestContext {
        lateinit var user: User
        lateinit var veriffSession: VeriffSession
    }
}
