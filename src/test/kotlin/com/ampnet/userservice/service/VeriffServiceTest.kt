package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.exception.VeriffReasonCode
import com.ampnet.userservice.exception.VeriffVerificationCode
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.model.VeriffDecision
import com.ampnet.userservice.persistence.model.VeriffSession
import com.ampnet.userservice.persistence.model.VeriffSessionState
import com.ampnet.userservice.persistence.repository.VeriffDecisionRepository
import com.ampnet.userservice.persistence.repository.VeriffSessionRepository
import com.ampnet.userservice.service.impl.UserMailServiceImpl
import com.ampnet.userservice.service.impl.UserServiceImpl
import com.ampnet.userservice.service.impl.VeriffServiceImpl
import com.ampnet.userservice.service.pojo.VeriffResponse
import com.ampnet.userservice.service.pojo.VeriffStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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

@Import(ApplicationProperties::class, RestTemplate::class)
class VeriffServiceTest : JpaServiceTestBase() {

    @Autowired
    lateinit var applicationProperties: ApplicationProperties

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Autowired
    lateinit var veriffSessionRepository: VeriffSessionRepository

    @Autowired
    lateinit var veriffDecisionRepository: VeriffDecisionRepository

    @Autowired
    @Qualifier("camelCaseObjectMapper")
    lateinit var camelCaseObjectMapper: ObjectMapper

    private lateinit var mockServer: MockRestServiceServer
    private val baseUrl = "http://localhost:8080"

    private val veriffService: VeriffServiceImpl by lazy {
        val userMailService = UserMailServiceImpl(mailTokenRepository, mailService)
        val userService = UserServiceImpl(
            userRepository, userInfoRepository, coopRepository, userMailService, passwordEncoder, applicationProperties
        )
        VeriffServiceImpl(
            veriffSessionRepository, veriffDecisionRepository,
            userInfoRepository, applicationProperties, userService, restTemplate, camelCaseObjectMapper
        )
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllUsers()
        databaseCleanerService.deleteAllVeriffSessions()
        databaseCleanerService.deleteAllVeriffDecisions()
        testContext = TestContext()
        mockServer = MockRestServiceServer.createServer(restTemplate)
    }

    @Test
    fun mustSaveUserData() {
        verify("Service will store valid user data") {
            val veriffResponse = getResourceAsText("/veriff/response.json")
            testContext.userInfo = veriffService.handleDecision(veriffResponse) ?: fail("Missing user info")
            assertThat(testContext.userInfo.sessionId).isEqualTo("12df6045-3846-3e45-946a-14fa6136d78b")
            assertThat(testContext.userInfo.firstName).isEqualTo("SARAH")
            assertThat(testContext.userInfo.lastName).isEqualTo("MORGAN")
        }
        verify("User data is stored") {
            assertThat(userInfoRepository.findById(testContext.userInfo.uuid)).isNotNull
        }
    }

    @Test
    fun mustReturnExistingExistingSessionForApprovedResponse() {
        suppose("User has veriff session") {
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
                ZonedDateTime.now(),
                VeriffSessionState.SUBMITTED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff decision is approved") {
            veriffPostedDecision("/veriff/response.json")
        }

        verify("Service will return url from stored veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid, baseUrl)
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
                ZonedDateTime.now(),
                VeriffSessionState.SUBMITTED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff posted resubmission decision") {
            veriffPostedDecision("/veriff/response-resubmission.json")
        }

        verify("Service will return url from stored veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid, baseUrl)
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
                ZonedDateTime.now(),
                VeriffSessionState.SUBMITTED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff posted declined decision") {
            veriffPostedDecision("/veriff/response-declined.json")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid, baseUrl)
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
                ZonedDateTime.now(),
                VeriffSessionState.SUBMITTED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff posted abandoned decision") {
            veriffPostedDecision("/veriff/response-abandoned.json")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid, baseUrl)
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
                ZonedDateTime.now(),
                VeriffSessionState.SUBMITTED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff posted expired decision") {
            veriffPostedDecision("/veriff/response-expired.json")
        }
        suppose("Veriff will return new session") {
            val response = getResourceAsText("/veriff/response-new-session.json")
            mockVeriffResponse(response, HttpMethod.POST, "/v1/sessions/")
        }

        verify("Service will return a new url veriff session") {
            val response = veriffService.getVeriffSession(testContext.user.uuid, baseUrl)
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
    fun mustHandleStartedEvent() {
        suppose("User has veriff session") {
            testContext.user =
                createUser("event@email.com", uuid = UUID.fromString("2652972e-2dfd-428a-93b9-3b283a0a754c"))
            val veriffSession = VeriffSession(
                "cbb238c6-51a0-482b-bd1a-42a2e0b0ff1c",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now(),
                VeriffSessionState.CREATED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }

        verify("Service will handle started event") {
            val data = getResourceAsText("/veriff/response-event-started.json")
            val session = veriffService.handleEvent(data) ?: fail("Missing session")
            assertThat(session.id).isEqualTo(testContext.veriffSession.id)
            assertThat(session.state).isEqualTo(VeriffSessionState.STARTED)
        }
    }

    @Test
    fun mustHandleSubmittedEvent() {
        suppose("User has veriff session") {
            testContext.user =
                createUser("event@email.com", uuid = UUID.fromString("2652972e-2dfd-428a-93b9-3b283a0a754c"))
            val veriffSession = VeriffSession(
                "cbb238c6-51a0-482b-bd1a-42a2e0b0ff1c",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now(),
                VeriffSessionState.CREATED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }

        verify("Service will handle started event") {
            val data = getResourceAsText("/veriff/response-event-submitted.json")
            val session = veriffService.handleEvent(data) ?: fail("Missing session")
            assertThat(session.id).isEqualTo(testContext.veriffSession.id)
            assertThat(session.state).isEqualTo(VeriffSessionState.SUBMITTED)
        }
    }

    @Test
    fun mustDeleteDecisionOnStartedEvent() {
        suppose("User has veriff session") {
            testContext.user =
                createUser("event@email.com", uuid = UUID.fromString("2652972e-2dfd-428a-93b9-3b283a0a754c"))
            val veriffSession = VeriffSession(
                "cbb238c6-51a0-482b-bd1a-42a2e0b0ff1c",
                testContext.user.uuid,
                "https://alchemy.veriff.com/v/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                testContext.user.uuid.toString(),
                "https://alchemy.veriff.com/",
                "created",
                false,
                ZonedDateTime.now(),
                VeriffSessionState.SUBMITTED
            )
            testContext.veriffSession = veriffSessionRepository.save(veriffSession)
        }
        suppose("Veriff posted resubmission decision") {
            testContext.veriffDecision = veriffPostedDecision("/veriff/response-resubmission-for-started-event.json")
        }

        verify("Service will handle started event") {
            val data = getResourceAsText("/veriff/response-event-started.json")
            val session = veriffService.handleEvent(data) ?: fail("Missing session")
            assertThat(session.id).isEqualTo(testContext.veriffSession.id)
            assertThat(session.state).isEqualTo(VeriffSessionState.STARTED)
        }
        verify("Resubmission decision is deleted") {
            val veriffDecision = veriffDecisionRepository.findById(testContext.veriffDecision.id)
            assertThat(veriffDecision).isNotPresent
        }
    }

    @Test
    fun mustThrowExceptionForMissingUser() {
        verify("Service will throw ResourceNotFoundException exception") {
            val exception = assertThrows<ResourceNotFoundException> {
                veriffService.getVeriffSession(UUID.randomUUID(), baseUrl)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_JWT_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionForMissingVerificationObject() {
        verify("Service will throw Veriff exception") {
            val veriffResponse = getResourceAsText("/veriff/response-missing-verification.json")
            assertThrows<VeriffException> {
                veriffService.handleDecision(veriffResponse)
            }
        }
    }

    @Test
    fun mustReturnNullUserInfoForDeclinedVerification() {
        verify("Service will return null for declined veriff response") {
            val veriffResponse = getResourceAsText("/veriff/response-declined.json")
            val userInfo = veriffService.handleDecision(veriffResponse)
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
            val veriffResponse = getResourceAsText("/veriff/response-with-vendor-data.json")
            testContext.userInfo = veriffService.handleDecision(veriffResponse) ?: fail("Missing user info")
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

    private fun veriffPostedDecision(file: String): VeriffDecision {
        val response = getResourceAsText(file)
        val veriffResponse: VeriffResponse = veriffService.mapVeriffResponse(response)
        val decision = VeriffDecision(veriffResponse.verification ?: fail("Missing verification"))
        return veriffDecisionRepository.save(decision)
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
        lateinit var veriffDecision: VeriffDecision
    }
}
