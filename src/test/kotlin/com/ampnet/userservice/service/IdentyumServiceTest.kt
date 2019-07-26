package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.config.RestTemplateConfig
import com.ampnet.userservice.controller.pojo.request.IdentyumPayloadRequest
import com.ampnet.userservice.service.impl.IdentyumServiceImpl
import com.ampnet.userservice.service.pojo.IdentyumDocumentModel
import com.ampnet.userservice.service.pojo.IdentyumUserModel
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate

@Import(JsonConfig::class, RestTemplateConfig::class, ApplicationProperties::class)
class IdentyumServiceTest : JpaServiceTestBase() {

    @Autowired
    lateinit var applicationProperties: ApplicationProperties
    @Autowired
    lateinit var restTemplate: RestTemplate

    private val identyumService: IdentyumServiceImpl by lazy {
        IdentyumServiceImpl(applicationProperties, restTemplate, objectMapper, userInfoRepository)
    }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToDecode() {
        val identyumPayloadRequest = loadIdentyumPayloadRequest()
        assertThat(identyumPayloadRequest.reportUuid).isEqualTo("8c99227d-5108-4b1d-bcd2-449826032f99")

        val identyumUser = loadDecodedIdentyumPayload()
        assertThat(identyumUser.identyumUuid).isEqualTo("ae1ee02d-8a2d-4c50-a6ca-8f0454e19f6d")

        val decryptedIdentyumUser = decryptPayload(identyumPayloadRequest)
        assertThat(decryptedIdentyumUser).isEqualTo(identyumUser)
    }

    @Test
    fun mustBeAbleToGenerateUserInfoFromIdentyumUser() {
        verify("Identyum request in proper format") {
            val userIdentyumJson = """
                {
                    "identyumUuid": "1234-1234-1234-1234",
                    "emails": [{
                        "type": "DEFAULT",
                        "email": "neki.mail@mail.com"
                    }],
                    "phones": [{
                        "type": "MOBILE",
                        "phoneNumber": "+38591234567"
                    }],
                    "document": [
                        {
                            "type": "PERSONAL_ID_CARD",
                            "countryCode": "HRV",
                            "firstName": "ime",
                            "lastName": "prezime",
                            "docNumber": "112661111",
                            "citizenship": "HRV",
                            "address": {
                                "city": "GRAD ZAGREB",
                                "county": "ZAGREB",
                                "streetAndNumber": "ULICA IZA 3"
                            },
                            "issuingAuthority": "PU ZAGREBAČKA",
                            "personalIdentificationNumber": {
                                "type": "OIB",
                                "value": "11111111111"
                            },
                            "resident": true,
                            "documentBilingual": false,
                            "permanent": false,
                            "docFrontImg": "base64 of image",
                            "docBackImg": "base64 of image",
                            "docFaceImg": "base64 of image",
                            "dateOfBirth": "1950-01-01",
                            "dateOfExpiry": "2021-01-11",
                            "dateOfIssue": "2016-01-11"
                        }
                    ]
                }
            """.trimIndent()
            testContext.identyumUser = objectMapper.readValue(userIdentyumJson)
            testContext.identyumDocument = testContext.identyumUser.document.first()
        }
        verify("Service can create UserInfo from IdentyumUser") {
            val userInfo = identyumService.createUserInfoFromIdentyumUser(testContext.identyumUser)
            assertThat(userInfo.identyumNumber).isEqualTo("1234-1234-1234-1234")
            assertThat(userInfo.verifiedEmail).isEqualTo("neki.mail@mail.com")
            assertThat(userInfo.phoneNumber).isEqualTo("+38591234567")
            assertThat(userInfo.documentType).isEqualTo("PERSONAL_ID_CARD")
            assertThat(userInfo.country).isEqualTo("HRV")
            assertThat(userInfo.firstName).isEqualTo("ime")
            assertThat(userInfo.lastName).isEqualTo("prezime")
            assertThat(userInfo.documentNumber).isEqualTo("112661111")
            assertThat(userInfo.citizenship).isEqualTo("HRV")
            assertThat(userInfo.addressCity).isEqualTo("GRAD ZAGREB")
            assertThat(userInfo.addressCounty).isEqualTo("ZAGREB")
            assertThat(userInfo.addressStreet).isEqualTo("ULICA IZA 3")
            assertThat(userInfo.resident).isEqualTo(true)
            assertThat(userInfo.dateOfBirth).isEqualTo("1950-01-01")
            assertThat(userInfo.webSessionUuid).isNull()
        }
    }

    @Test
    fun mustRemoveImagesFromDecryptedPayload() {
        verify("Image json data is removed") {
            val userIdentyumJson = """
                    {
                        "identyumUuid": "1234-1234-1234-1234",
                        "document": [
                            {
                                "resident": true,
                                "documentBilingual": false,
                                "permanent": false,
                                "docFrontImg": "base64 of image",
                                "docBackImg": "base64 of image",
                                "docFaceImg": "base64 of image"
                            }
                        ]
                    }
                """.trimIndent()
            val removed = identyumService.removeDocumentImageData(userIdentyumJson)
            assertThat(removed).doesNotContain("docFrontImg", "docBackImg", "docFaceImg", "base64 of image")
        }
    }

    private fun decryptPayload(identyumPayloadRequest: IdentyumPayloadRequest): IdentyumUserModel {
        val decryptedData = identyumService.decrypt(
            identyumPayloadRequest.payload, "12345abcde", identyumPayloadRequest.reportUuid)
        return objectMapper.readValue(decryptedData)
    }

    private fun loadIdentyumPayloadRequest(): IdentyumPayloadRequest {
        val identyumResponse = getResourceAsText("/identyum/identyum-response.json")
        return objectMapper.readValue(identyumResponse)
    }

    private fun loadDecodedIdentyumPayload(): IdentyumUserModel {
        val decodedPayload = getResourceAsText("/identyum/payload.json")
        return objectMapper.readValue(decodedPayload)
    }

    private class TestContext {
        lateinit var identyumUser: IdentyumUserModel
        lateinit var identyumDocument: IdentyumDocumentModel
    }
}
