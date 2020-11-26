package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.config.RestTemplateConfig
import com.ampnet.userservice.exception.ReCaptchaException
import com.ampnet.userservice.service.impl.ReCaptchaServiceImpl
import com.ampnet.userservice.service.pojo.ReCaptchaRequest
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.DefaultResponseCreator
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate

@Import(ApplicationProperties::class, JsonConfig::class, RestTemplateConfig::class)
@RunWith(SpringRunner::class)
class ReCaptchaServiceTest : JpaServiceTestBase() {

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Autowired
    private lateinit var restTemplate: RestTemplate

    private val service: ReCaptchaService by lazy {
        ReCaptchaServiceImpl(applicationProperties, restTemplate, objectMapper)
    }

    private lateinit var mockServer: MockRestServiceServer
    private val reCaptchaToken = "token"

    @Test
    fun mustNotThrowExceptionIfTokenValidationIsSuccessful() {
        suppose("ReCAPTCHA validation is successful") {
            mockReCaptchaGoogleResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), generateSuccessfulGoogleResponse())
        }
        suppose("ReCaptcha Service doesn't return exception") {
            service.validateResponseToken(reCaptchaToken)
        }
        verify("Rest template called mocked server") { mockServer.verify() }
    }

    @Test
    fun mustThrowExceptionIfTokenValidationIsNotSuccessful() {
        suppose("ReCAPTCHA validation failed") {
            mockReCaptchaGoogleResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), generateUnSuccessfulGoogleResponse())
        }
        verify("ReCaptcha Service throws exception") {
            assertThrows<ReCaptchaException> { service.validateResponseToken(reCaptchaToken) }
        }
        verify("Rest template called mocked server") { mockServer.verify() }
    }

    @Test
    fun mustThrowExceptionIfReCaptchaScoreIsTooLow() {
        suppose("ReCAPTCHA validation failed") {
            mockReCaptchaGoogleResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), generateLowScoreGoogleResponse())
        }
        verify("ReCaptcha Service throws exception due to low score") {
            assertThrows<ReCaptchaException> { service.validateResponseToken(reCaptchaToken) }
        }
        verify("Rest template called mocked server") { mockServer.verify() }
    }

    @Test
    fun mustThrowExceptionIfGoogleServerReturnsEmptyResponse() {
        suppose("ReCAPTCHA validation failed") {
            mockReCaptchaGoogleResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), "")
        }
        verify("ReCaptcha Service throws exception due to error while reading google response") {
            assertThrows<ReCaptchaException> { service.validateResponseToken(reCaptchaToken) }
        }
        verify("Rest template called mocked server") { mockServer.verify() }
    }

    private fun mockReCaptchaGoogleResponse(status: DefaultResponseCreator, body: String) {
        val request = ReCaptchaRequest(
            applicationProperties.reCaptcha.secret,
            reCaptchaToken
        )
        mockServer = MockRestServiceServer.createServer(restTemplate)
        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(applicationProperties.reCaptcha.url)
        )
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andExpect(
                MockRestRequestMatchers.content()
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockRestRequestMatchers.content().json(objectMapper.writeValueAsString(request)))
            .andRespond(status.body(body))
    }

    private fun generateSuccessfulGoogleResponse(): String =
        """
            {
                "success":"true",
                "challenge_ts":"56743453",
                "hostname":"user_hostname",
                "score": "0.6",
                "error-codes": []
            }
        """.trimIndent()

    private fun generateUnSuccessfulGoogleResponse(): String =
        """
            {
                "success":"false",
                "error-codes": [
                    "missing-input-response",
                    "missing-input-secret"
                ]
            }
        """.trimIndent()

    private fun generateLowScoreGoogleResponse(): String =
        """
            {
                "success":"true",
                "challenge_ts":"56743453",
                "hostname":"user_hostname",
                "score": "0.4",
                "error-codes": []
            }
        """.trimIndent()
}
