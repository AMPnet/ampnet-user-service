package com.ampnet.userservice.service

import com.ampnet.userservice.COOP
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.service.impl.CoopServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile

@Import(JsonConfig::class)
class CoopServiceTest : JpaServiceTestBase() {

    private val service: CoopService by lazy { CoopServiceImpl(coopRepository, objectMapper, cloudStorageService) }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllCoop()
        testContext = TestContext()
    }

    @Test
    fun mustThrowExceptionForDuplicatedCoopIdentifier() {
        suppose("Coop exists") {
            testContext.coop = createCoop(COOP)
        }

        verify("Service will throw exception for existing coop") {
            val request = CoopRequest(COOP, "name", "hostname", null, "reCAPTCHAtoken")
            val logoMock = MockMultipartFile("logo", "logo.png", "image/png", "LogoData".toByteArray())
            val exception = assertThrows<ResourceAlreadyExistsException> {
                service.createCoop(request, logoMock)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.COOP_EXISTS)
        }
    }

    private class TestContext {
        lateinit var coop: Coop
    }
}
