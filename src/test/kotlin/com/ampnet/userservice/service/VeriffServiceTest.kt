package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.service.impl.UserServiceImpl
import com.ampnet.userservice.service.impl.VeriffServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.util.UUID

@Import(JsonConfig::class, ApplicationProperties::class)
class VeriffServiceTest : JpaServiceTestBase() {

    @Autowired
    lateinit var applicationProperties: ApplicationProperties

    private val veriffService: VeriffServiceImpl by lazy {
        val userService = UserServiceImpl(
            userRepository, userInfoRepository, mailTokenRepository, coopRepository,
            mailService, passwordEncoder, applicationProperties
        )
        VeriffServiceImpl(userInfoRepository, applicationProperties, userService)
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
            testContext.userInfo = veriffService.saveUserVerificationData(veriffResponse) ?: fail("Missing user info")
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
            assertThat(userInfoRepository.findById(testContext.userInfo.uuid)).isNotNull
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
    fun mustReturnNullUserInfoForDeclinedVerification() {
        verify("Service will return null for declined veriff response") {
            val veriffResponse = getResourceAsText("/veriff/response-declined.json")
            val userInfo = veriffService.saveUserVerificationData(veriffResponse)
            assertThat(userInfo).isNull()
        }
    }

    @Test
    fun mustVerifyUserForValidVendorData() {
        suppose("There is unverified user") {
            testContext.user =
                createUser("email@gfas.co", uuid = UUID.fromString("5750f893-29fa-4910-8304-62f834338f47"))
        }

        verify("Service will store valid user data") {
            databaseCleanerService.deleteAllUserInfos()
            val veriffResponse = getResourceAsText("/veriff/response-with-vendor-data.json")
            testContext.userInfo = veriffService.saveUserVerificationData(veriffResponse) ?: fail("Missing user info")
        }
        verify("User is verified") {
            val user = userRepository.findById(testContext.user.uuid).get()
            assertThat(user.userInfoUuid).isNotNull()
        }
        verify("User info is connected") {
            val userInfo = userInfoRepository.findById(testContext.userInfo.uuid).get()
            assertThat(userInfo.connected).isTrue()
        }
    }

    private class TestContext {
        lateinit var userInfo: UserInfo
        lateinit var user: User
    }
}
