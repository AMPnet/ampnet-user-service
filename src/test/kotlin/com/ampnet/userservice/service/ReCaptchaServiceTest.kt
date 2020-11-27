package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.config.RestTemplateConfig
import com.ampnet.userservice.exception.ReCaptchaException
import com.ampnet.userservice.service.impl.ReCaptchaServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.DefaultResponseCreator
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Import(ApplicationProperties::class, JsonConfig::class, RestTemplateConfig::class)
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

    @BeforeEach
    fun initTestContext() {
        applicationProperties.reCaptcha.enabled = true
    }

    @Test
    fun mustNotThrowExceptionIfTokenVerificationIsSuccessful() {
        suppose("ReCAPTCHA verification is successful") {
            mockReCaptchaGoogleResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), generateSuccessfulGoogleResponse())
        }
        suppose("ReCaptcha Service doesn't return exception") {
            service.validateResponseToken(reCaptchaToken)
        }

        verify("Rest template called mocked server") { mockServer.verify() }
    }

    @Test
    fun mustThrowExceptionIfTokenVerificationIsNotSuccessful() {
        suppose("ReCAPTCHA verification failed") {
            mockReCaptchaGoogleResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), generateUnSuccessfulGoogleResponse())
        }

        verify("ReCaptcha Service throws exception") {
            assertThrows<ReCaptchaException> { service.validateResponseToken(reCaptchaToken) }
        }
        verify("Rest template called mocked server") { mockServer.verify() }
    }

    @Test
    fun mustThrowExceptionIfReCaptchaScoreIsTooLow() {
        suppose("ReCAPTCHA verification failed") {
            mockReCaptchaGoogleResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), generateLowScoreGoogleResponse())
        }

        verify("ReCaptcha Service throws exception due to low score") {
            assertThrows<ReCaptchaException> { service.validateResponseToken(reCaptchaToken) }
        }
        verify("Rest template called mocked server") { mockServer.verify() }
    }

    @Test
    fun mustThrowExceptionIfGoogleServerReturnsEmptyResponse() {
        suppose("ReCAPTCHA verification failed") {
            mockReCaptchaGoogleResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), "")
        }

        verify("ReCaptcha Service throws exception due to error while reading google's response") {
            assertThrows<ReCaptchaException> { service.validateResponseToken(reCaptchaToken) }
        }
        verify("Rest template called mocked server") { mockServer.verify() }
    }

    @Test
    fun mustThrowExceptionIfTokenIsNull() {
        verify("ReCaptcha Service throws exception if token is null") {
            assertThrows<ReCaptchaException> { service.validateResponseToken(null) }
        }
    }

    private fun mockReCaptchaGoogleResponse(status: DefaultResponseCreator, body: String) {
        mockServer = MockRestServiceServer.createServer(restTemplate)
        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(generateGoogleUri(reCaptchaToken))
        )
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andRespond(status.body(body))
    }

    private fun generateGoogleUri(reCaptchaToken: String): URI {
        return UriComponentsBuilder
            .fromHttpUrl(applicationProperties.reCaptcha.url)
            .queryParam("secret", applicationProperties.reCaptcha.secret)
            .queryParam("response", reCaptchaToken)
            .build()
            .toUri()
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
