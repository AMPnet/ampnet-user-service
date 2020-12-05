package com.ampnet.userservice.controller

import com.ampnet.userservice.config.ApplicationProperties
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class VeriffControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private val veriffPath = "/veriff/webhook"
    private val xClientHeader = "X-AUTH-CLIENT"
    private val xSignature = "X-SIGNATURE"

    @Test
    fun mustStoreUserInfoFromVeriff() {
        suppose("Veriff config is set") {
            applicationProperties.veriff.apiKey = "8a733721-9bb3-48b1-90b9-6463ac1493eb"
            applicationProperties.veriff.privateKey = "8a733721-9bb3-48b1-90b9-6463ac1493eb"
        }
        suppose("User has no user info") {
            databaseCleanerService.deleteAllUserInfos()
        }

        verify("Controller will accept valid data") {
            val request = getResourceAsText("/veriff/response.json")
            mockMvc.perform(
                post(veriffPath)
                    .content(request)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(xClientHeader, applicationProperties.veriff.apiKey)
                    .header(xSignature, "e73fe0d8b416861d42c6839ec126e7bc7b020c1c19ff7df93dfc96b66e81b5c8")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
        }
        verify("User info is stored") {
            val userInfo = userInfoRepository.findBySessionId("12df6045-3846-3e45-946a-14fa6136d78b")
            assert(userInfo.isPresent)
        }
    }
}
