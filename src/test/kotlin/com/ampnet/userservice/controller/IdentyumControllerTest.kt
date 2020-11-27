package com.ampnet.userservice.controller

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.security.WithMockCrowdfundUser
import com.ampnet.userservice.service.pojo.IdentyumTokenRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class IdentyumControllerTest : ControllerTestBase() {

    private val identyumPath = "/identyum"
    private val identyumTokenPath = "/identyum/token"
    private val headerSignature = "signature"
    private val headerSecretKey = "secret-key"
    private val clientSessionUuid = "cdb1e44e-db55-4bdc-8c4e-1e68b1793780"
    private val identyumTokenResponse =
        "{\"access_token\":\"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIwSTczcVJlS3dCWG82VDRVSHg5M0s1VzJ5cXJNODRBemxKQnNxZEVlTXhRIn0.eyJleHAiOjE1ODc2NDgyNjksImlhdCI6MTU4NzY0NjQ2OSwianRpIjoiM2U3MGU1ZGEtNDY3ZC00ZTk4LWE3NGYtMzZjNjM4ZGQ2NDVhIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvYXV0aC9yZWFsbXMvY2xpZW50cyIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIyMDFkYzIzNy01OGNiLTRkZjUtYjUyYi04ZjJkMjc4OTFmZmQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJpZGVudHl1bS1jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiYjRiZDY0NjctY2I2ZS00YjNjLTkwYzctYzAzMWQyNzI5YTU0IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJsb2dpbl9zbXMiLCJjbGllbnQiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJkZWJ1ZyI6InRydWUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IkFNUG5ldCBJTyIsInByZWZlcnJlZF91c2VybmFtZSI6ImFtcG5ldF9zdGFnZSIsImdpdmVuX25hbWUiOiJBTVBuZXQiLCJsb2NhbGUiOiJlbiIsImZhbWlseV9uYW1lIjoiSU8iLCJlbWFpbCI6Im1pc2xhdkBhbXBuZXQuaW8ifQ.C5eSkL59NhYGDicE3Yar_If72vx_Ii2sz7FpXK9SQmYLjNHLxIGc_F9C3VkCuZHM0-NmtGziK5f6NfBBknbE0fVV-KkjMp4QlqXUvk75QYLX_14hqowZPSE973MYd1rv3Vet0XiZ-mI8emKRESldUaxLfOLJbTWY-y3kcRRQrGySDxF4jnRiVoi8r4FMQmFNgZsytw3SXtz7inlo8G99rOgM8QSvxHU3A1RGnE3eztjl1koiG8P58jABABNQ-fv31A0W_zgwSLVnLEp5LHNX2Cx2v-ypjfQz58uFd4Fi5J9JlYBvjssMJD-n7GH87mqi1HhvTmJPBYuTLW4Wi7619w\",\"expires_in\":1800,\"refresh_expires_in\":1800,\"refresh_token\":\"eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIzMmI0OWU2ZC0yNGZhLTRjYmQtOTc3OC00NmJmYzZiMWQxM2MifQ.eyJleHAiOjE1ODc2NDgyNjksImlhdCI6MTU4NzY0NjQ2OSwianRpIjoiZmI4ZDZkOTUtMWU0ZS00MGJkLThjODgtZTFjZGQ1MTQ3MmM2IiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgwODAvYXV0aC9yZWFsbXMvY2xpZW50cyIsImF1ZCI6Imh0dHA6Ly9rZXljbG9hazo4MDgwL2F1dGgvcmVhbG1zL2NsaWVudHMiLCJzdWIiOiIyMDFkYzIzNy01OGNiLTRkZjUtYjUyYi04ZjJkMjc4OTFmZmQiLCJ0eXAiOiJSZWZyZXNoIiwiYXpwIjoiaWRlbnR5dW0tY2xpZW50Iiwic2Vzc2lvbl9zdGF0ZSI6ImI0YmQ2NDY3LWNiNmUtNGIzYy05MGM3LWMwMzFkMjcyOWE1NCIsInNjb3BlIjoiZW1haWwgcHJvZmlsZSJ9.mOFw52MrGgZChNQ160s2PZpJSbxu-oqEde9ZfqcroWA\",\"session_state\":\"b4bd6467-cb6e-4b3c-90c7-c031d2729a54\"}"

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Test
    @WithMockCrowdfundUser
    fun mustBeAbleToGetIdentyumToken() {
        suppose("Identyum will return token") {
            mockIdentyumResponse(MockRestResponseCreators.withStatus(HttpStatus.OK), identyumTokenResponse)
        }

        verify("User can get Identyum token") {
            val result = mockMvc.perform(get(identyumTokenPath))
                .andExpect(status().isOk)
                .andReturn()
            assertThat(result.response.contentAsString).isEqualTo(identyumTokenResponse)
            mockServer.verify()
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustGetErrorIfIdentyumReturnsServerError() {
        suppose("Identyum will return error") {
            mockIdentyumResponse(MockRestResponseCreators.withServerError())
        }

        verify("Controller will return Identyum token error code") {
            val result = mockMvc.perform(get(identyumTokenPath))
                .andExpect(status().isBadGateway)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.REG_IDENTYUM_TOKEN)
        }
    }

    @Test
    @WithMockCrowdfundUser
    fun mustGetErrorIfIdentyumReturnsNoContent() {
        suppose("Identyum will return error") {
            mockIdentyumResponse(MockRestResponseCreators.withNoContent())
        }

        verify("Controller will return Identyum token error code") {
            val result = mockMvc.perform(get(identyumTokenPath))
                .andExpect(status().isBadGateway)
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.REG_IDENTYUM_TOKEN)
        }
    }

    @Test
    fun mustBeToProcessIdentyumRequest() {
        assumeTrue(
            applicationProperties.identyum.ampnetPrivateKey.startsWith("-----BEGIN PRIVATE KEY-----"),
            "Missing ampnet private key in application.properties"
        )

        suppose("UserInfo repository is empty") {
            databaseCleanerService.deleteAllUserInfos()
        }

        verify("Controller will handle Identyum request") {
            val identyumResponse = getResourceAsText("/identyum/encrypted.txt")
            val identyumSignature = getResourceAsText("/identyum/signature.txt")
            val identyumSecretKey = getResourceAsText("/identyum/secretKey.txt")
            mockMvc.perform(
                post(identyumPath)
                    .header(headerSignature, identyumSignature)
                    .header(headerSecretKey, identyumSecretKey)
                    .content(identyumResponse)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andExpect(status().isOk)
        }
        verify("UserInfo is created") {
            val optionalUserInfo = userInfoRepository.findByClientSessionUuid(clientSessionUuid)
            assertThat(optionalUserInfo).isPresent
        }
    }

    @Test
    fun mustThrowErrorForExistingWebSessionUuid() {
        assumeTrue(
            applicationProperties.identyum.ampnetPrivateKey.startsWith("-----BEGIN PRIVATE KEY-----"),
            "Missing ampnet private key in application.properties"
        )

        suppose("UserInfo exists") {
            databaseCleanerService.deleteAllUserInfos()
            val userInfo = createUserInfo(clientSessionUuid = clientSessionUuid)
            userInfoRepository.save(userInfo)
        }

        verify("Controller will return error for existing webSessionUuid") {
            val identyumResponse = getResourceAsText("/identyum/encrypted.txt")
            val identyumSignature = getResourceAsText("/identyum/signature.txt")
            val identyumSecretKey = getResourceAsText("/identyum/secretKey.txt")
            val response = mockMvc.perform(
                post(identyumPath)
                    .header(headerSignature, identyumSignature)
                    .header(headerSecretKey, identyumSecretKey)
                    .content(identyumResponse)
                    .contentType(MediaType.APPLICATION_JSON)
            )
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
            val signature = "c2lnbmF0dXJl"
            val secretKey = "c2VjcmV0LWtleQ=="
            val request =
                """
                {
                    "clientSessionUuid":"cdb1e44e-db55-4bdc-8c4e-1e68b1793780",
                    "userSessionUuid":"fe8ca142-0dbd-4882-b30d-95139b152f94",
                    "userUuid":"a36ddb9f-e2eb-4769-a5eb-df54a6aa12db",
                    "reportUuid":"2172b736-4dcd-4a23-952b-b7c2b3116d87",
                    "client":"zvoc",
                    "status":"FINISHED",
                    "ordinal":1,
                    "data":{
                      "personalData":{ }
                    }
                }
                """.trimIndent()
            mockMvc.perform(
                post(identyumPath)
                    .header(headerSignature, signature)
                    .header(headerSecretKey, secretKey)
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
            ).andExpect(status().isUnprocessableEntity)
        }
    }

    @Test
    @Disabled("Not for automated testing")
    fun getIdentyumToken() {
        verify("User can get Identyum token") {
            val result = mockMvc.perform(get(identyumTokenPath))
                .andExpect(status().isOk)
                .andReturn()
            assertThat(result.response).isNotNull
        }
    }

    private fun mockIdentyumResponse(status: DefaultResponseCreator, body: String = "") {
        val request = IdentyumTokenRequest(
            applicationProperties.identyum.username,
            applicationProperties.identyum.password
        )

        mockServer = MockRestServiceServer.createServer(restTemplate)
        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(applicationProperties.identyum.url)
        )
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andExpect(
                MockRestRequestMatchers.content()
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(MockRestRequestMatchers.content().json(objectMapper.writeValueAsString(request)))
            .andRespond(status.body(body))
    }
}
