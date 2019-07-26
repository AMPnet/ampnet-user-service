package com.ampnet.userservice.controller

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.response.IdentyumTokenResponse
import com.ampnet.userservice.exception.ErrorCode
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.DefaultResponseCreator
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

class IdentyumControllerTest : ControllerTestBase() {

    private val identyumPath = "/identyum"
    private val identyumTokenPath = "/identyum/token"
    private val webSessionUuid = "17ac3c1d-2793-4ed3-b92c-8e9e3471582c"

    @Autowired
    private lateinit var restTemplate: RestTemplate
    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private lateinit var mockServer: MockRestServiceServer

    @Test
    fun mustBeAbleToGetIdentyumToken() {
        suppose("Identyum will return token") {
            mockIdentyumResponse(MockRestResponseCreators.withStatus(HttpStatus.OK),
                    "1c03b4a5-6f2b-4de5-a3e7-cd043177bc95")
        }

        verify("User can get Identyum token") {
            val result = mockMvc.perform(get(identyumTokenPath))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
            val response = objectMapper.readValue<IdentyumTokenResponse>(result.response.contentAsString)
            assertThat(response).isNotNull
            assertThat(response.token).isEqualTo("1c03b4a5-6f2b-4de5-a3e7-cd043177bc95")

            mockServer.verify()
        }
    }

    @Test
    fun mustGetErrorIfIdentyumReturnsServerError() {
        suppose("Identyum will return error") {
            mockIdentyumResponse(MockRestResponseCreators.withServerError())
        }

        verify("Controller will return Identyum token error code") {
            val result = mockMvc.perform(get(identyumTokenPath))
                    .andExpect(MockMvcResultMatchers.status().isBadGateway)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.REG_IDENTYUM_TOKEN)
        }
    }

    @Test
    fun mustGetErrorIfIdentyumReturnsNoContent() {
        suppose("Identyum will return error") {
            mockIdentyumResponse(MockRestResponseCreators.withNoContent())
        }

        verify("Controller will return Identyum token error code") {
            val result = mockMvc.perform(get(identyumTokenPath))
                    .andExpect(MockMvcResultMatchers.status().isBadGateway)
                    .andReturn()
            verifyResponseErrorCode(result, ErrorCode.REG_IDENTYUM_TOKEN)
        }
    }

    @Test
    fun mustBeToProcessIdentyumRequest() {
        suppose("UserInfo repository is empty") {
            databaseCleanerService.deleteAllUserInfos()
        }

        verify("Controller will handle Identyum request") {
            val identyumResponse = getResourceAsText("/identyum/identyum-response.json")
            mockMvc.perform(post(identyumPath)
                    .content(identyumResponse)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
        }
        verify("UserInfo is created") {
            val optionalUserInfo = userInfoRepository.findByWebSessionUuid(webSessionUuid)
            assertThat(optionalUserInfo).isPresent
        }
    }

    @Test
    fun mustThrowErrorForExistingWebSessionUuid() {
        suppose("UserInfo exists") {
            databaseCleanerService.deleteAllUserInfos()
            val userInfo = createUserInfo(webSessionUuid = webSessionUuid)
            userInfoRepository.save(userInfo)
        }

        verify("Controller will return error for existing webSessionUuid") {
            val identyumResponse = getResourceAsText("/identyum/identyum-response.json")
            val response = mockMvc.perform(post(identyumPath)
                    .content(identyumResponse)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
                .andReturn()
            verifyResponseErrorCode(response, ErrorCode.REG_IDENTYUM_EXISTS)
        }
    }

    @Test
    fun mustUnprocessableEntityForInvalidIdentyumData() {
        suppose("UserInfo repository is empty") {
            databaseCleanerService.deleteAllUserInfos()
        }

        verify("Controller will return unprocessable entity for invalid payload") {
            val request = """
                {
                    "webSessionUuid": "17ac3c1d-2793-4ed3-b92c-8e9e3471582c",
                    "productUuid": "dc40b0a2-06be-4f39-8f36-27e83e905ffb",
                    "reportUuid": "8c99227d-5108-4b1d-bcd2-449826032f99",
                    "reportName": "DEFAULT_REPORT",
                    "version": 1,
                    "outputFormat": "json",
                    "payloadFormat": "json",
                    "processStatus": "SUCCESS",
                    "payload": "aW52YWxpZC1wYXlsb2Fk",
                    "payloadSignature": "example_signature",
                    "tsCreated": 1559712417000
                }
            """.trimIndent()
            mockMvc.perform(post(identyumPath)
                .content(request)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity)
        }
    }

    @Test
    @Disabled("Not for automated testing")
    fun getIdentyumToken() {
        verify("User can get Identyum token") {
            val result = mockMvc.perform(get(identyumTokenPath))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
            val response = objectMapper.readValue<IdentyumTokenResponse>(result.response.contentAsString)
            assertThat(response).isNotNull
            assertThat(response.token).isNotEmpty()
        }
    }

    private fun mockIdentyumResponse(status: DefaultResponseCreator, body: String = "") {
        val map = LinkedMultiValueMap<String, String>()
        map["username"] = applicationProperties.identyum.username
        map["password"] = applicationProperties.identyum.password

        mockServer = MockRestServiceServer.createServer(restTemplate)
        mockServer.expect(ExpectedCount.once(),
                MockRestRequestMatchers.requestTo(applicationProperties.identyum.url))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content()
                        .contentType("application/x-www-form-urlencoded;charset=UTF-8"))
                .andExpect(MockRestRequestMatchers.content()
                        .formData(map))
                .andRespond(status.body(body))
    }
}
