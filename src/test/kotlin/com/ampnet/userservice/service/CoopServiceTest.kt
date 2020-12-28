package com.ampnet.userservice.service

import com.ampnet.userservice.COOP
import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InternalException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.service.impl.CoopServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class CoopServiceTest : JpaServiceTestBase() {

    @Autowired
    lateinit var applicationProperties: ApplicationProperties
    private val service: CoopService by lazy { CoopServiceImpl(coopRepository, objectMapper, cloudStorageService, applicationProperties) }
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
        suppose("Coop creating is enabled") {
            applicationProperties.coop.enableCreating = true
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

    @Test
    fun mustThrowExceptionForCoopCreatingDisabled() {
        suppose("Coop exists") {
            testContext.coop = createCoop(COOP)
        }
        suppose("Coop creating is disabled") {
            applicationProperties.coop.enableCreating = false
        }

        verify("Service will throw exception if coop creating is disabled") {
            val request = CoopRequest("another coop", "name", "hostname", null, "reCAPTCHAtoken")
            val logoMock = MockMultipartFile("logo", "logo.png", "image/png", "LogoData".toByteArray())
            val exception = assertThrows<InternalException> {
                service.createCoop(request, logoMock)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.COOP_CREATING_DISABLED)
        }
    }

    private class TestContext {
        lateinit var coop: Coop
    }
}
