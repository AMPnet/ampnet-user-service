package com.ampnet.userservice.controller

import com.ampnet.userservice.COOP
import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.enums.KycProvider
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ReCaptchaException
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.security.WithMockCrowdfundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

class CoopControllerTest : ControllerTestBase() {

    private val coopPath = "/coop"
    private val publicPath = "/public"
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllCoop()
        testContext = TestContext()
    }

    @Test
    fun mustCreateCoop() {
        suppose("ReCAPTCHA verification is successful") {
            Mockito.`when`(reCaptchaService.validateResponseToken(testContext.reCaptchaToken)).then { Unit }
        }
        suppose("Cloud storage service will store logo") {
            testContext.logoMock = MockMultipartFile("logo", "logo.png", "image/png", "LogoData".toByteArray())
            mockFileStorage(testContext.logoMock, testContext.logoLink)
        }
        suppose("Cloud storage service will store banner") {
            testContext.bannerMock = MockMultipartFile("banner", "banner.png", "image/png", "BannerData".toByteArray())
            mockFileStorage(testContext.bannerMock, testContext.bannerLink)
        }

        verify("User can create coop") {
            testContext.name = "New Coop a"
            testContext.identifier = "new-coop-a"
            val configMap: Map<String, Any> = objectMapper.readValue(testContext.config)
            val request = CoopRequest(
                testContext.identifier,
                testContext.name,
                testContext.hostname,
                configMap,
                testContext.reCaptchaToken
            )
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(request)
            )
            val builder = getMultipartRequestBuilder(coopPath)
            val result = mockMvc.perform(
                builder.file(requestJson)
                    .file(testContext.logoMock)
                    .file(testContext.bannerMock)
            )
                .andExpect(status().isOk)
                .andReturn()

            val coopResponse: CoopResponseTest = objectMapper.readValue(result.response.contentAsString)
            assertThat(coopResponse.identifier).isEqualTo(testContext.identifier)
            assertThat(coopResponse.name).isEqualTo(testContext.name)
            assertThat(coopResponse.createdAt).isBefore(ZonedDateTime.now())
            assertThat(coopResponse.hostname).isEqualTo(testContext.hostname)
            assertThat(serializeConfig(coopResponse.config)).isEqualTo(testContext.config)
            assertThat(coopResponse.logo).isEqualTo(testContext.logoLink)
            assertThat(coopResponse.banner).isEqualTo(testContext.bannerLink)
            assertThat(coopResponse.kycProvider).isEqualTo(testContext.kycProvider)
        }
        verify("Coop is created") {
            val coop = coopRepository.findAll().first()
            assertThat(coop.name).isEqualTo(testContext.name)
            assertThat(coop.identifier).isEqualTo(testContext.identifier)
            assertThat(coop.createdAt).isBefore(ZonedDateTime.now())
            assertThat(coop.hostname).isEqualTo(testContext.hostname)
            assertThat(coop.config).isEqualTo(testContext.config)
            assertThat(coop.kycProvider).isEqualTo(testContext.kycProvider)
            assertThat(coop.logo).isEqualTo(testContext.logoLink)
            assertThat(coop.banner).isEqualTo(testContext.bannerLink)
        }
    }

    @Test
    fun mustGetErrorIfReCaptchaReturnsError() {
        suppose("ReCAPTCHA verification failed") {
            Mockito.`when`(reCaptchaService.validateResponseToken(testContext.reCaptchaToken))
                .thenAnswer { throw ReCaptchaException("ReCAPTCHA verification failed") }
        }

        verify("Controller will return ReCaptcha error code") {
            val request = CoopRequest("new-coop-a", "New Coop a", null, null, testContext.reCaptchaToken)
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(request)
            )
            val builder = getMultipartRequestBuilder(coopPath)
            val result = mockMvc.perform(
                builder.file(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.REG_RECAPTCHA)
        }
    }

    @Test
    @WithMockCrowdfundUser(role = UserRole.ADMIN, coop = COOP)
    fun mustBeAbleToUpdateCoop() {
        suppose("There is coop") {
            testContext.coop = createCoop(COOP)
        }
        suppose("Cloud storage service will store logo") {
            testContext.logoMock = MockMultipartFile("logo", "logo.png", "image/png", "LogoData".toByteArray())
            mockFileStorage(testContext.logoMock, testContext.logoLink)
        }
        suppose("Cloud storage service will store banner") {
            testContext.bannerMock = MockMultipartFile("banner", "banner.png", "image/png", "BannerData".toByteArray())
            mockFileStorage(testContext.bannerMock, testContext.bannerLink)
        }

        verify("Admin can update coop") {
            testContext.hostname = "new.my.host"
            testContext.name = "New name"
            testContext.kycProvider = KycProvider.VERIFF
            testContext.config =
                """
                    {
                        "colors": {
                            "main": "brown"
                        },
                        "arkane": "STAGING",
                        "test": false,
                        "retry": 1
                    }
                """.replace("\\s".toRegex(), "")
            val configMap: Map<String, Any> = objectMapper.readValue(testContext.config)
            val request = CoopUpdateRequest(
                testContext.name, testContext.hostname, false, configMap, testContext.kycProvider
            )
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(request)
            )
            val builder = getMultipartRequestBuilder(coopPath, HttpMethod.PUT)
            val result = mockMvc.perform(
                builder.file(requestJson)
                    .file(testContext.logoMock)
                    .file(testContext.bannerMock)
            )
                .andExpect(status().isOk)
                .andReturn()

            val coopResponse: CoopResponseTest = objectMapper.readValue(result.response.contentAsString)
            assertThat(coopResponse.name).isEqualTo(testContext.name)
            assertThat(coopResponse.identifier).isEqualTo(COOP)
            assertThat(coopResponse.hostname).isEqualTo(testContext.hostname)
            assertThat(coopResponse.needUserVerification).isEqualTo(false)
            assertThat(coopResponse.logo).isEqualTo(testContext.logoLink)
            assertThat(coopResponse.banner).isEqualTo(testContext.bannerLink)
            assertThat(coopResponse.kycProvider).isEqualTo(testContext.kycProvider)
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
            assertThat(coopResponse.kycProvider).isEqualTo(testContext.coop.kycProvider)
        }
    }

    @Test
    @WithMockCrowdfundUser(role = UserRole.ADMIN, coop = COOP)
    fun mustDeleteCacheOnUpdateCoopRequest() {
        suppose("There is coop") {
            testContext.coop = createCoop(COOP)
        }
        suppose("Coop is cached on requesting coop by host") {
            mockMvc.perform(
                get("$publicPath/app/config/hostname/${testContext.coop.hostname}")
            )
                .andExpect(status().isOk)
        }
        suppose("Coop is cached on requesting coop by identifier") {
            mockMvc.perform(
                get("$publicPath/app/config/identifier/${testContext.coop.identifier}")
            )
                .andExpect(status().isOk)
        }
        suppose("Admin updates coop") {
            testContext.hostname = "new.my.host"
            testContext.name = "New name"
            val configMap: Map<String, Any> = objectMapper.readValue(testContext.config)
            val request = CoopUpdateRequest(
                testContext.name, testContext.hostname, null, configMap, testContext.kycProvider
            )
            val requestJson = MockMultipartFile(
                "request", "request.json", "application/json",
                objectMapper.writeValueAsBytes(request)
            )
            val builder = getMultipartRequestBuilder(coopPath, HttpMethod.PUT)
            mockMvc.perform(builder.file(requestJson))
                .andExpect(status().isOk)
                .andReturn()
        }

        verify("Cache is deleted on update coop request") {
            val hostname = testContext.coop.hostname ?: fail("Hostname not defined")
            val coopCacheByHost = cacheManager.getCache(COOP_CACHE)?.get(hostname)?.get()
            val coopCacheByIdentifier = cacheManager.getCache(COOP_CACHE)?.get(testContext.coop.identifier)?.get()
            assertThat(coopCacheByHost).isNull()
            assertThat(coopCacheByIdentifier).isNull()
        }
    }

    private fun getMultipartRequestBuilder(
        urlPath: String,
        method: HttpMethod = HttpMethod.POST
    ): MockMultipartHttpServletRequestBuilder {
        return MockMvcRequestBuilders.multipart(urlPath).apply {
            with { request ->
                request.method = method.name
                request
            }
        }
    }

    private fun mockFileStorage(file: MockMultipartFile, link: String) {
        Mockito.`when`(cloudStorageService.saveFile(file.originalFilename, file.bytes))
            .thenReturn(link)
    }

    private class TestContext {
        lateinit var name: String
        lateinit var identifier: String
        lateinit var coop: Coop
        var hostname = "ampnet.io"
        var config =
            """
                {
                    "title": "AMPnet",
                    "logo_url": "http://assets/logo-amp.png",
                    "icon_url": "http://assets/favicon.ico",
                    "arkane": {
                        "id": "AMPnet",
                        "env": "staging"
                    },
                    "identyum": {
                        "startLanguage": "en"
                    },
                    "googleClientId": "google-id",
                    "facebookAppId": "facebook-id"
                }
            """.replace("\\s".toRegex(), "")
        var reCaptchaToken = "token"
        lateinit var logoMock: MockMultipartFile
        lateinit var bannerMock: MockMultipartFile
        val logoLink = "logo-link"
        val bannerLink = "banner-link"
        var kycProvider = KycProvider.VERIFF
    }
}
