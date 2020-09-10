package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.config.RestTemplateConfig
import com.ampnet.userservice.service.impl.IdentyumServiceImpl
import com.ampnet.userservice.service.pojo.IdentyumInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assumptions.assumeTrue
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

    @Test
    fun mustRemoveImagesFromDecryptedPayload() {
        verify("Image json data is removed") {
            val userIdentyumJson =
                """
                {
                    "clientSessionUuid":"cdb1e44e-db55-4bdc-8c4e-1e68b1793780",
                    "userSessionUuid":"fe8ca142-0dbd-4882-b30d-95139b152f94",
                    "userUuid":"a36ddb9f-e2eb-4769-a5eb-df54a6aa12db",
                    "reportUuid":"2172b736-4dcd-4a23-952b-b7c2b3116d87",
                    "client":"zvoc",
                    "status":"FINISHED",
                    "ordinal":1,
                    "data": "data",
                    "images": [
                        {
                            "uuid":"fbdf3de8-a3fc-4e5f-88ab-f2fd28322eb9",
                            "base64": "base64data"
                        }
                    ]
                }
                """.trimIndent()
            val removed = identyumService.removeImages(userIdentyumJson)
            assertThat(removed).doesNotContain("images", "base64", "base64data")
        }
    }

    @Test
    fun mustBeAbleToDecodeReport() {
        assumeTrue(
            applicationProperties.identyum.ampnetPrivateKey.startsWith("-----BEGIN PRIVATE KEY-----"),
            "Missing ampnet private key in application.properties"
        )

        verify("Service can decode report") {
            val encryptedReport = getResourceAsText("/identyum/encrypted.txt")
            val secretKey = getResourceAsText("/identyum/secretKey.txt")
            val signature = getResourceAsText("/identyum/signature.txt")
            val decryptedReport = identyumService.decryptReport(encryptedReport, secretKey, signature)
            val originalJson = getResourceAsText("/identyum/original.json")
            assertThat(decryptedReport).isEqualTo(originalJson)
        }
    }

    @Test
    fun mustBeAbleToMapIdentyumJson() {
        verify("Service can map Identyum JSON to UserInfo") {
            val json = getResourceAsText("/identyum/original.json")
            val identyumInput: IdentyumInput = identyumService.mapReport(json)
            assertThat(identyumInput.userSessionUuid.toString()).isEqualTo("fe8ca142-0dbd-4882-b30d-95139b152f94")
            assertThat(identyumInput.clientSessionUuid.toString()).isEqualTo("cdb1e44e-db55-4bdc-8c4e-1e68b1793780")
            assertThat(identyumInput.userUuid.toString()).isEqualTo("a36ddb9f-e2eb-4769-a5eb-df54a6aa12db")
        }
    }
}
