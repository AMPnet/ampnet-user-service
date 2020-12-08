package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.exception.VeriffReasonCode
import com.ampnet.userservice.exception.VeriffVerificationCode
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.model.VeriffSession
import com.ampnet.userservice.persistence.repository.VeriffSessionRepository
import com.ampnet.userservice.service.impl.UserServiceImpl
import com.ampnet.userservice.service.impl.VeriffServiceImpl
import com.ampnet.userservice.service.pojo.VeriffStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate
import java.time.ZonedDateTime
import java.util.UUID

@Import(JsonConfig::class, ApplicationProperties::class, RestTemplate::class)
class VeriffServiceTest : JpaServiceTestBase() {

    @Autowired
    lateinit var applicationProperties: ApplicationProperties

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Autowired
    lateinit var veriffSessionRepository: VeriffSessionRepository

    private lateinit var mockServer: MockRestServiceServer

    private val veriffService: VeriffServiceImpl by lazy {
        val userService = UserServiceImpl(
            userRepository, userInfoRepository, mailTokenRepository, coopRepository,
            mailService, passwordEncoder, applicationProperties
        )
        VeriffServiceImpl(veriffSessionRepository, userInfoRepository, applicationProperties, userService, restTemplate)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        testContext = TestContext()
        mockServer = MockRestServiceServer.createServer(restTemplate)
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
        verify("User data is stored") {
            assertThat(userInfoRepository.findById(testContext.userInfo.uuid)).isNotNull
        }
    }

    @Test
    fun mustReturnExistingExistingSessionForApprovedResponse() {
        suppose("User has veriff session") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user =
                createUser("approved@email.com", uuid = UUID.fromString("5750f893-29fa-4910-8304-62f834338f47"))
            val veriffSession = VeriffSession(
                "12df6045-3846-3e45-946a-14fa6136d78b",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com",
                "created",
                false,
                ZonedDateTime.now()
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff will return decision response") {
            val response = getResourceAsText("/veriff/response.json")
            mockVeriffResponse(response, HttpMethod.GET, "/v1/sessions/${testContext.veriffSession.id}/decision")
        }

        verify("Service will return url from stored veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.approved)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.POSITIVE.code)
            assertThat(decision.reasonCode).isNull()
            assertThat(decision.reason).isNull()
            assertThat(decision.acceptanceTime).isNotNull()
            assertThat(decision.decisionTime).isNotNull()
        }
    }

    @Test
    fun mustReturnExistingExistingSessionForResubmissionResponse() {
        suppose("User has veriff session") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user =
                createUser("resubmission@email.com", uuid = UUID.fromString("e363cff9-3ab1-4017-b2d6-c47e17e143bc"))
            val veriffSession = VeriffSession(
                "32599b8e-e596-4601-973c-aa197ae0dfde",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now()
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff will return decision response") {
            val response = getResourceAsText("/veriff/response-resubmission.json")
            mockVeriffResponse(response, HttpMethod.GET, "/v1/sessions/${testContext.veriffSession.id}/decision")
        }

        verify("Service will return url from stored veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.resubmission_requested)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.RESUBMISSION.code)
            assertThat(decision.reasonCode).isEqualTo(VeriffReasonCode.DOC_NOT_VISIBLE.code)
            assertThat(decision.reason).isNotNull()
            assertThat(decision.acceptanceTime).isNotNull()
            assertThat(decision.decisionTime).isNotNull()
        }
    }

    @Test
    fun mustCreateNewSessionForDeclinedResponse() {
        suppose("User has veriff session") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user =
                createUser("resubmission@email.com", uuid = UUID.fromString("4c2c2950-7a20-4fd7-b37f-f1d63a8211b4"))
            val veriffSession = VeriffSession(
                "44927492-8799-406e-8076-933bc9164ebc",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now()
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff will return decision response") {
            val response = getResourceAsText("/veriff/response-declined.json")
            mockVeriffResponse(response, HttpMethod.GET, "/v1/sessions/${testContext.veriffSession.id}/decision")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isNotEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.declined)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.NEGATIVE.code)
            assertThat(decision.reasonCode).isEqualTo(VeriffReasonCode.DOC_NOT_USED.code)
            assertThat(decision.reason).isNotNull()
            assertThat(decision.acceptanceTime).isNotNull()
            assertThat(decision.decisionTime).isNotNull()
        }
        verify("New veriff session is created") {
            val veriffSessions = veriffSessionRepository.findByUserUuidOrderByCreatedAtDesc(testContext.user.uuid)
            assertThat(veriffSessions).hasSize(2)
            verifyNewVeriffSession(veriffSessions.first())
        }
    }

    @Test
    fun mustCreateNewSessionForAbandonedResponse() {
        suppose("User has veriff session") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user =
                createUser("resubmission@email.com", uuid = UUID.fromString("5d4633ee-d770-45f9-85af-0692fd82daac"))
            val veriffSession = VeriffSession(
                "4c0be76b-d01d-46d5-8c07-a0e2629ebd86",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now()
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff will return decision response") {
            val response = getResourceAsText("/veriff/response-abandoned.json")
            mockVeriffResponse(response, HttpMethod.GET, "/v1/sessions/${testContext.veriffSession.id}/decision")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isNotEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.abandoned)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.NEGATIVE_EXPIRED.code)
            assertThat(decision.reasonCode).isNull()
            assertThat(decision.reason).isNull()
            assertThat(decision.acceptanceTime).isNotNull()
            assertThat(decision.decisionTime).isNull()
        }
        verify("New veriff session is created") {
            val veriffSessions = veriffSessionRepository.findByUserUuidOrderByCreatedAtDesc(testContext.user.uuid)
            assertThat(veriffSessions).hasSize(2)
            verifyNewVeriffSession(veriffSessions.first())
        }
    }

    @Test
    fun mustCreateNewSessionForExpiredResponse() {
        suppose("User has veriff session") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user =
                createUser("resubmission@email.com", uuid = UUID.fromString("5d4633ee-d770-45f9-85af-0692fd82daac"))
            val veriffSession = VeriffSession(
                "eb52789e-1cce-4c26-a86e-d111bb75bd27",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now()
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff will return decision response") {
            val response = getResourceAsText("/veriff/response-expired.json")
            mockVeriffResponse(response, HttpMethod.GET, "/v1/sessions/${testContext.veriffSession.id}/decision")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isNotEqualTo(testContext.veriffSession.url)
            val decision = response.decision ?: fail("Missing decision")
            assertThat(decision.sessionId).isEqualTo(testContext.veriffSession.id)
            assertThat(decision.status).isEqualTo(VeriffStatus.expired)
            assertThat(decision.code).isEqualTo(VeriffVerificationCode.NEGATIVE_EXPIRED.code)
            assertThat(decision.reasonCode).isNull()
            assertThat(decision.reason).isNull()
            assertThat(decision.acceptanceTime).isNotNull()
            assertThat(decision.decisionTime).isNull()
        }
        verify("New veriff session is created") {
            val veriffSessions = veriffSessionRepository.findByUserUuidOrderByCreatedAtDesc(testContext.user.uuid)
            assertThat(veriffSessions).hasSize(2)
            verifyNewVeriffSession(veriffSessions.first())
        }
    }

    @Test
    fun mustCreateNewSessionForUnknownVeriffStatus() {
        suppose("User has veriff session") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllVeriffSessions()
            testContext.user =
                createUser("resubmission@email.com", uuid = UUID.fromString("5d4633ee-d770-45f9-85af-0692fd82daac"))
            val veriffSession = VeriffSession(
                "eb52789e-1cce-4c26-a86e-d111bb75bd27",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now()
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff will return decision response") {
            val response = getResourceAsText("/veriff/response-missing-verification.json")
            mockVeriffResponse(response, HttpMethod.GET, "/v1/sessions/${testContext.veriffSession.id}/decision")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid)
                ?: fail("Service didn't return session")
            assertThat(response.verificationUrl).isNotEqualTo(testContext.veriffSession.url)
            assertThat(response.decision).isNull()
        }
        verify("New veriff session is created") {
            val veriffSessions = veriffSessionRepository.findByUserUuidOrderByCreatedAtDesc(testContext.user.uuid)
            assertThat(veriffSessions).hasSize(2)
            verifyNewVeriffSession(veriffSessions.first())
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
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllUserInfos()
            testContext.user =
                createUser("email@gfas.co", uuid = UUID.fromString("5750f893-29fa-4910-8304-62f834338f47"))
        }

        verify("Service will store valid user data") {
            val veriffResponse = getResourceAsText("/veriff/response-with-vendor-data.json")
            testContext.userInfo = veriffService.saveUserVerificationData(veriffResponse) ?: fail("Missing user info")
        }
        verify("User is verified") {
            val user = userRepository.findById(testContext.user.uuid).get()
            assertThat(user.userInfoUuid).isEqualTo(testContext.userInfo.uuid)
        }
        verify("User info is connected") {
            val userInfo = userInfoRepository.findById(testContext.userInfo.uuid).get()
            assertThat(userInfo.connected).isTrue()
        }
    }

    private fun verifyNewVeriffSession(veriffSession: VeriffSession) {
        assertThat(veriffSession.id).isEqualTo("47679394-b37d-4932-86e6-d751f45ae546")
        assertThat(veriffSession.url)
            .isEqualTo("https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new-url")
        assertThat(veriffSession.vendorData).isEqualTo("5ad36a1b-1c9e-4bf9-a88f-3c7fe68bdcf5")
    }

    private fun mockVeriffResponse(body: String, method: HttpMethod, path: String) {
        val status = MockRestResponseCreators.withStatus(HttpStatus.OK)
        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(applicationProperties.veriff.baseUrl + path)
        )
            .andExpect(MockRestRequestMatchers.method(method))
            .andRespond(status.body(body).contentType(MediaType.APPLICATION_JSON))
    }

    private class TestContext {
        lateinit var userInfo: UserInfo
        lateinit var user: User
        lateinit var veriffSession: VeriffSession
    }
}
