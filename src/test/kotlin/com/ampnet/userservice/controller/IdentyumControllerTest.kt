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
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.DefaultResponseCreator
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

class IdentyumControllerTest : ControllerTestBase() {

    private val identyumTokenPath = "/identyum/token"

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
