package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.MailCheckRequest
import com.ampnet.userservice.controller.pojo.response.MailCheckResponse
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ErrorResponse
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.MailTokenRepository
import com.ampnet.userservice.security.WithMockCrowdfoundUser
import com.ampnet.userservice.service.MailService
import com.ampnet.userservice.service.SocialService
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

@ActiveProfiles("SocialMockConfig")
class RegistrationControllerTest : ControllerTestBase() {

    private val pathSignup = "/signup"
    private val confirmationPath = "/mail-confirmation"
    private val resendConfirmationPath = "/mail-confirmation/resend"
    private val checkMail = "/mail-check"

    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var socialService: SocialService
    @Autowired
    private lateinit var mailTokenRepository: MailTokenRepository
    @Autowired
    private lateinit var mailService: MailService

    private lateinit var testUser: TestUser
    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        testUser = TestUser()
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToSignUpUser() {
        suppose("User info exists") {
            databaseCleanerService.deleteAllUsers()
            createUserInfo(email = testUser.email, webSessionUuid = testUser.webSessionUuid)
        }
        suppose("The user send request to sign up") {
            val requestJson = generateSignupJson()
            testContext.mvcResult = mockMvc.perform(
                    post(pathSignup)
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(testContext.mvcResult.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testUser.email)
            testUser.uuid = UUID.fromString(userResponse.uuid)
        }
        verify("The user is stored in database") {
            val userInRepo = userService.find(testUser.uuid) ?: fail("User must not be null")
            assert(userInRepo.email == testUser.email)
            assertThat(testUser.uuid).isEqualTo(userInRepo.uuid)
            assert(passwordEncoder.matches(testUser.password, userInRepo.password))
            assertThat(userInRepo.authMethod).isEqualTo(testUser.authMethod)
            assert(userInRepo.role.id == UserRoleType.USER.id)
            assert(userInRepo.createdAt.isBefore(ZonedDateTime.now()))
            assertThat(userInRepo.enabled).isFalse()
        }
        verify("The user confirmation token is created") {
            val userInRepo = userService.find(testUser.uuid) ?: fail("User must not be null")
            val mailToken = mailTokenRepository.findByUserUuid(userInRepo.uuid)
            assertThat(mailToken).isPresent
            assertThat(mailToken.get().token).isNotNull()
            assertThat(mailToken.get().createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testContext.mailConfirmationToken = mailToken.get().token.toString()
        }
        verify("Sending mail was initiated") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendConfirmationMail(testUser.email, testContext.mailConfirmationToken)
        }
    }

    @Test
    fun incompleteSignupRequestShouldFail() {
        verify("The user cannot send malformed request to sign up") {
            val requestJson = """
            |{
                |"signup_method" : "EMAIL",
                |"user_info" : {
                    |"email" : "filipduj@gmail.com"
                |}
            |}""".trimMargin()

            mockMvc.perform(
                    post(pathSignup)
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
        }
    }

    @Test
    fun invalidEmailSignupRequestShouldFail() {
        verify("The user cannot send request with invalid email") {
            testUser.email = "invalid-mail.com"
            testUser.password = "passssword"
            val invalidJsonRequest = generateSignupJson()

            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_INVALID)
        }
    }

    @Test
    fun shortPasswordSignupRequestShouldFail() {
        verify("The user cannot send request with too short passowrd") {
            testUser.email = "invalid@mail.com"
            testUser.password = "short"
            val invalidJsonRequest = generateSignupJson()

            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(invalidJsonRequest)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_INVALID)
        }
    }

    @Test
    fun signupShouldFailIfUserAlreadyExists() {
        suppose("User exists in database") {
            databaseCleanerService.deleteAllUsers()
            saveTestUser()
        }

        verify("The user cannot sign up with already existing email") {
            val requestJson = generateSignupJson()
            val result = mockMvc.perform(
                    post(pathSignup)
                            .content(requestJson)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest)
                    .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()

            val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
            val expectedErrorCode = getResponseErrorCode(ErrorCode.REG_USER_EXISTS)
            assert(response.errCode == expectedErrorCode)
        }
    }

    @Test
    fun signupUsingFacebookMethod() {
        suppose("User info exists") {
            databaseCleanerService.deleteAllUsers()
            createUserInfo(email = testUser.email, webSessionUuid = testUser.webSessionUuid)
        }
        suppose("Social service is mocked to return Facebook user") {
            testContext.socialEmail = "johnsmith@gmail.com"
            Mockito.`when`(socialService.getFacebookEmail(testContext.token))
                    .thenReturn(testContext.socialEmail)
        }

        verify("The user can sign up with Facebook account") {
            verifySocialSignUp(AuthMethod.FACEBOOK, testContext.token, testContext.socialEmail, testUser.webSessionUuid)
        }
    }

    @Test
    fun signupUsingGoogleMethod() {
        suppose("User info exists") {
            databaseCleanerService.deleteAllUsers()
            createUserInfo(email = testUser.email, webSessionUuid = testUser.webSessionUuid)
        }
        suppose("Social service is mocked to return Google user") {
            testContext.socialEmail = "johnsmith@gmail.com"
            Mockito.`when`(socialService.getGoogleEmail(testContext.token))
                    .thenReturn(testContext.socialEmail)
        }

        verify("The user can sign up with Google account") {
            verifySocialSignUp(AuthMethod.GOOGLE, testContext.token, testContext.socialEmail, testUser.webSessionUuid)
        }
    }

    @Test
    fun mustBeAbleToConfirmEmail() {
        suppose("The user is created with unconfirmed email") {
            createUnconfirmedUser()
        }

        verify("The user can confirm email with mail token") {
            val mailToken = mailTokenRepository.findByUserUuid(testUser.uuid)
            assertThat(mailToken).isPresent

            mockMvc.perform(get("$confirmationPath?token=${mailToken.get().token}"))
                    .andExpect(status().isOk)
        }
        verify("The user is confirmed in database") {
            val user = userService.find(testUser.uuid) ?: fail("User must not be null")
            assertThat(user.enabled).isTrue()
        }
    }

    @Test
    fun mustGetBadRequestForInvalidTokenFormat() {
        verify("Invalid token format will get bad response") {
            mockMvc.perform(get("$confirmationPath?token=bezvezni-token-tak"))
                    .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun mustGetNotFoundRandomToken() {
        verify("Random token will get not found response") {
            val randomToken = UUID.randomUUID().toString()
            mockMvc.perform(get("$confirmationPath?token=$randomToken"))
                    .andExpect(status().isNotFound)
        }
    }

    @Test
    fun mustNotBeAbleToConfirmEmailWithExpiredToken() {
        suppose("The user is created with unconfirmed email") {
            createUnconfirmedUser()
        }
        suppose("The token has expired") {
            val optionalMailToken = mailTokenRepository.findByUserUuid(testUser.uuid)
            assertThat(optionalMailToken).isPresent
            val mailToken = optionalMailToken.get()
            mailToken.createdAt = ZonedDateTime.now().minusDays(2)
            mailTokenRepository.save(mailToken)
        }

        verify("The user cannot confirm email with expired token") {
            val optionalMailToken = mailTokenRepository.findByUserUuid(testUser.uuid)
            assertThat(optionalMailToken).isPresent
            mockMvc.perform(get("$confirmationPath?token=${optionalMailToken.get().token}"))
                    .andExpect(status().isBadRequest)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToResendConfirmationEmail() {
        suppose("The user has confirmation mail token") {
            testUser.email = defaultEmail
            createUnconfirmedUser()
            val optionalMailToken = mailTokenRepository.findByUserUuid(testUser.uuid)
            assertThat(optionalMailToken).isPresent
        }

        verify("User can request resend mail confirmation") {
            mockMvc.perform(get(resendConfirmationPath))
                    .andExpect(status().isOk)
        }
        verify("The user confirmation token is created") {
            val userInRepo = userService.find(testUser.uuid) ?: fail("User must not be null")
            val mailToken = mailTokenRepository.findByUserUuid(userInRepo.uuid)
            assertThat(mailToken).isPresent
            assertThat(mailToken.get().token).isNotNull()
            assertThat(mailToken.get().createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testContext.mailConfirmationToken = mailToken.get().token.toString()
        }
        verify("Sending mail was initiated") {
            Mockito.verify(mailService, Mockito.times(1))
                    .sendConfirmationMail(testUser.email, testContext.mailConfirmationToken)
        }
        verify("The user can confirm mail with new token") {
            val mailToken = mailTokenRepository.findByUserUuid(testUser.uuid)
            assertThat(mailToken).isPresent

            mockMvc.perform(get("$confirmationPath?token=${mailToken.get().token}"))
                    .andExpect(status().isOk)
        }
        verify("The user is confirmed in database") {
            val userInRepo = userService.find(testUser.uuid) ?: fail("User must not be null")
            assertThat(userInRepo.enabled).isTrue()
        }
    }

    @Test
    fun unauthorizedUserCannotResendConfirmationEmail() {
        verify("User will get error unauthorized") {
            mockMvc.perform(get(resendConfirmationPath)).andExpect(status().isUnauthorized)
        }
    }

    @Test
    fun mustReturnFalseForUnusedEmail() {
        suppose("Email is not used") {
            databaseCleanerService.deleteAllUsers()
        }

        verify("User will get false for non existing email") {
            val request = MailCheckRequest("missing@email.com")
            val result = mockMvc.perform(
                    post(checkMail)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk)
                    .andReturn()

            val response: MailCheckResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.email).isEqualTo(request.email)
            assertThat(response.userExists).isFalse()
        }
    }

    @Test
    fun mustReturnTrueIfEmailIsUsed() {
        suppose("User exists") {
            databaseCleanerService.deleteAllUsers()
            saveTestUser()
        }

        verify("User will get true for used email") {
            val request = MailCheckRequest(testUser.email)
            val result = mockMvc.perform(
                    post(checkMail)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk)
                    .andReturn()

            val response: MailCheckResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(response.email).isEqualTo(request.email)
            assertThat(response.userExists).isTrue()
        }
    }

    @Test
    fun mustReturnErrorForInvalidEmailFormat() {
        verify("System will reject invalid Email format") {
            val request = MailCheckRequest("invalid-format@")
            mockMvc.perform(
                    post(checkMail)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isBadRequest)
        }
    }

    private fun createUnconfirmedUser() {
        databaseCleanerService.deleteAllUsers()
        createUserInfo(email = testUser.email, webSessionUuid = testUser.webSessionUuid)
        val request = CreateUserServiceRequest(
            testUser.webSessionUuid, testUser.email, testUser.password, testUser.authMethod)
        val savedUser = userService.createUser(request)
        testUser.uuid = savedUser.uuid
        val user = userService.find(testUser.uuid) ?: fail("User must not be null")
        assertThat(user.enabled).isFalse()
    }

    private fun generateSignupJson(): String {
        return """
            |{
            |  "web_session_uuid" : "${testUser.webSessionUuid}",
            |  "signup_method" : "${testUser.authMethod}",
            |  "user_info" : {
            |       "email" : "${testUser.email}",
            |       "password" : "${testUser.password}"
            |   }
            |}
        """.trimMargin()
    }

    private fun verifySocialSignUp(authMethod: AuthMethod, token: String, email: String, webSessionUuid: String) {
        suppose("User has obtained token on frontend and sends signup request") {
            val request = """
            |{
            |  "web_session_uuid" : "$webSessionUuid",
            |  "signup_method" : "$authMethod",
            |  "user_info" : {
            |    "token" : "$token"
            |  }
            |}
            """.trimMargin()

            testContext.mvcResult = mockMvc.perform(
                    post(pathSignup)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isOk)
                    .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(testContext.mvcResult.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(email)
        }

        verify("The user is stored in database") {
            val userInRepo = userService.find(email) ?: fail("User must not be null")
            assert(userInRepo.email == email)
            assert(userInRepo.role.id == UserRoleType.USER.id)
            assertThat(userInRepo.enabled).isTrue()
        }
    }

    private fun saveTestUser(): User {
        val userInfo = createUserInfo(email = testUser.email, webSessionUuid = testUser.webSessionUuid)
        val user = createUser(testUser.email, testUser.authMethod, testUser.password, userInfo)
        testUser.uuid = user.uuid
        return user
    }

    private class TestUser {
        var uuid: UUID = UUID.randomUUID()
        var email = "john@smith.com"
        var password = "abcdefgh"
        var authMethod = AuthMethod.EMAIL
        var webSessionUuid = "1234-1234-1234-1234"
    }

    private class TestContext {
        lateinit var mvcResult: MvcResult
        val token = "token"
        lateinit var socialEmail: String
        lateinit var mailConfirmationToken: String
    }
}
