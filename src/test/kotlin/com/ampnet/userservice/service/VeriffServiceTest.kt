package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.service.impl.VeriffServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(JsonConfig::class, ApplicationProperties::class)
class VeriffServiceTest : JpaServiceTestBase() {

    @Autowired
    lateinit var applicationProperties: ApplicationProperties

    private val veriffService: VeriffServiceImpl by lazy {
        VeriffServiceImpl(userInfoRepository, applicationProperties)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
    }

    @Test
    fun mustSaveUserData() {
        verify("Service will store valid user data") {
            databaseCleanerService.deleteAllUserInfos()
            val veriffResponse = getResourceAsText("/veriff/response.json")
            testContext.userInfo = veriffService.saveUserVerificationData(veriffResponse)
            assertThat(testContext.userInfo.sessionId).isEqualTo("12df6045-3846-3e45-946a-14fa6136d78b")
            assertThat(testContext.userInfo.firstName).isEqualTo("SARAH")
            assertThat(testContext.userInfo.lastName).isEqualTo("MORGAN")
            assertThat(testContext.userInfo.dateOfBirth).isEqualTo("1967-03-30")
            assertThat(testContext.userInfo.placeOfBirth).isEqualTo("MADRID")
            assertThat(testContext.userInfo.document.type).isEqualTo("DRIVERS_LICENSE")
            assertThat(testContext.userInfo.document.number).isEqualTo("MORGA753116SM9IJ")
            assertThat(testContext.userInfo.document.country).isEqualTo("GB")
            assertThat(testContext.userInfo.document.validUntil).isEqualTo("2022-04-20")
        }
        verify("Correct user data is stored") {
            assertThat(userInfoRepository.findById(testContext.userInfo.id)).isNotNull
        }
    }

    @Test
    fun mustThrowExceptionForMissingVerificationObject() {
        verify("Service will throw Veriff exception") {
            val veriffResponse = getResourceAsText("/veriff/response-missing-verification.json")
            assertThrows<VeriffException> {
                veriffService.saveUserVerificationData(veriffResponse)
            }
        }
    }

    @Test
    fun mustThrowExceptionForDeclinedVerification() {
        verify("Service will throw Veriff exception") {
            val veriffResponse = getResourceAsText("/veriff/response-declined.json")
            assertThrows<VeriffException> {
                veriffService.saveUserVerificationData(veriffResponse)
            }
        }
    }

    private class TestContext {
        lateinit var userInfo: UserInfo
    }
}
