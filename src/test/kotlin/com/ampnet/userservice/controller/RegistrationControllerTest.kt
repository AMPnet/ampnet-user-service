package com.ampnet.userservice.controller

import com.ampnet.userservice.COOP
import com.ampnet.userservice.amqp.mailservice.UserDataWithToken
import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ErrorResponse
import com.ampnet.userservice.exception.ReCaptchaException
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.MailTokenRepository
import com.ampnet.userservice.security.WithMockCrowdfundUser
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class RegistrationControllerTest : ControllerTestBase() {

    private val pathSignup = "/signup"
    private val confirmationPath = "/mail-confirmation"
    private val resendConfirmationPath = "/mail-confirmation/resend"

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var mailTokenRepository: MailTokenRepository

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private lateinit var testUser: TestUser
    private lateinit var testContext: TestContext

    private val coop: Coop by lazy {
        databaseCleanerService.deleteAllCoop()
        createCoop(COOP)
    }

    @BeforeEach
    fun initTestData() {
        databaseCleanerService.deleteAllUsers()
        testUser = TestUser()
        testContext = TestContext()
        coop.identifier
        applicationProperties.reCaptcha.enabled = false
    }

    @Test
    fun mustBeAbleToSignUpUser() {
        suppose("The user sends request to sign up") {
            val requestJson = generateSignupJson()
            testContext.mvcResult = mockMvc.perform(
                post(pathSignup)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
            assert(userInRepo.role.id == UserRole.USER.id)
            assert(userInRepo.createdAt.isBefore(ZonedDateTime.now()))
            assertThat(userInRepo.enabled).isFalse()
            testContext.user = userInRepo
        }
        verify("The user confirmation token is created") {
            val userInRepo = userService.find(testUser.uuid) ?: fail("User must not be null")
            val mailToken = mailTokenRepository.findByUserUuid(userInRepo.uuid)
            assertThat(mailToken).isPresent
            assertThat(mailToken.get().token).isNotNull()
            assertThat(mailToken.get().createdAt).isBeforeOrEqualTo(ZonedDateTime.now())

            testContext.mailConfirmationToken = mailToken.get().token
        }
        verify("Sending mail was initiated") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendConfirmationMail(UserDataWithToken(testContext.user, testContext.mailConfirmationToken))
        }
    }

    @Test
    fun mustGetErrorIfReCaptchaReturnsError() {
        suppose("ReCAPTCHA verification failed") {
            Mockito.`when`(reCaptchaService.validateResponseToken(testUser.reCaptchaToken))
                .thenAnswer { throw ReCaptchaException("ReCAPTCHA verification failed") }
        }

        verify("Controller will return ReCaptcha error code") {
            val requestJson = generateSignupJson()
            val result = mockMvc.perform(
                post(pathSignup)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
            verifyResponseErrorCode(result, ErrorCode.REG_RECAPTCHA)
        }
    }

    @Test
    fun signUpWithoutCoop() {
        verify("The user cannot send malformed request to sign up") {
            val requestJson =
                """
                |{
                |  "signup_method" : "${testUser.authMethod}",
                |  "user_info" : {
                |       "first_name": "${testUser.first}",
                |       "last_name": "${testUser.last}",
                |       "email" : "${testUser.email}",
                |       "password" : "${testUser.password}"
                |   },
                |   "re_captcha_token" : "${testUser.reCaptchaToken}"
                |}""".trimMargin()
            testContext.mvcResult = mockMvc.perform(
                post(pathSignup)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()
        }
        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(testContext.mvcResult.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testUser.email)
            assertThat(userResponse.coop).isEqualTo(applicationProperties.coop.default)
            testUser.uuid = UUID.fromString(userResponse.uuid)
        }
        verify("The user is stored in database") {
            val userInRepo = userService.find(testUser.uuid) ?: fail("User must not be null")
            assert(userInRepo.email == testUser.email)
            assert(passwordEncoder.matches(testUser.password, userInRepo.password))
            assertThat(userInRepo.authMethod).isEqualTo(testUser.authMethod)
            assert(userInRepo.role == UserRole.USER)
            assert(userInRepo.createdAt.isBefore(ZonedDateTime.now()))
            assertThat(userInRepo.enabled).isFalse()
            assertThat(userInRepo.coop).isEqualTo(applicationProperties.coop.default)
        }
    }

    @Test
    fun incompleteSignupRequestShouldFail() {
        verify("The user cannot send malformed request to sign up") {
            val requestJson =
                """
            |{
                |"signup_method" : "EMAIL",
                |"user_info" : {
                    |"email" : "filipduj@gmail.com"
                |}
            |}""".trimMargin()

            mockMvc.perform(
                post(pathSignup)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
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
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.INT_REQUEST)
        }
    }

    @Test
    fun shortPasswordSignupRequestShouldFail() {
        verify("The user cannot send request with too short password") {
            testUser.email = "invalid@mail.com"
            testUser.password = "short"
            val invalidJsonRequest = generateSignupJson()

            val result = mockMvc.perform(
                post(pathSignup)
                    .content(invalidJsonRequest)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.INT_REQUEST)
        }
    }

    @Test
    fun signupShouldFailIfUserAlreadyExists() {
        suppose("User exists in database") {
            saveTestUser()
        }

        verify("The user cannot sign up with already existing email") {
            val requestJson = generateSignupJson()
            val result = mockMvc.perform(
                post(pathSignup)
                    .content(requestJson)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
            val expectedErrorCode = getResponseErrorCode(ErrorCode.REG_USER_EXISTS)
            assert(response.errCode == expectedErrorCode)
        }
    }

    @Test
    fun signupUsingFacebookMethod() {
        suppose("Social service is mocked to return Facebook user") {
            testContext.socialEmail = "johnsmith@gmail.com"
            Mockito.`when`(socialService.getFacebookEmail(testContext.token))
                .thenReturn(generateSocialUser(testContext.socialEmail))
        }

        verify("The user can sign up with Facebook account") {
            verifySocialSignUp(AuthMethod.FACEBOOK, testContext.token, testContext.socialEmail)
        }
    }

    @Test
    fun signupUsingGoogleMethod() {
        suppose("Social service is mocked to return Google user") {
            testContext.socialEmail = "johnsmith@gmail.com"
            Mockito.`when`(socialService.getGoogleEmail(testContext.token))
                .thenReturn(generateSocialUser(testContext.socialEmail))
        }

        verify("The user can sign up with Google account") {
            verifySocialSignUp(AuthMethod.GOOGLE, testContext.token, testContext.socialEmail)
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
    @WithMockCrowdfundUser(coop = COOP)
    fun mustBeAbleToResendConfirmationEmail() {
        suppose("The user has confirmation mail token") {
            testUser.email = defaultEmail
            testContext.user = createUnconfirmedUser()
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

            testContext.mailConfirmationToken = mailToken.get().token
        }
        verify("Sending mail was initiated") {
            Mockito.verify(mailService, Mockito.times(1))
                .sendConfirmationMail(UserDataWithToken(testContext.user, testContext.mailConfirmationToken))
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
            val result = mockMvc.perform(get(resendConfirmationPath))
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.JWT_FAILED)
        }
    }

    @Test
    fun mustReturnNotFoundMailForNonExistingWebSessionUuid() {
        verify("Controller will return not found for non existing web session uuid") {
            val webSessionUuid = UUID.randomUUID()
            mockMvc.perform(get("/mail-user-pending/$webSessionUuid"))
                .andExpect(status().isNotFound)
        }
    }

    private fun createUnconfirmedUser(): User {
        val request = CreateUserServiceRequest(
            testUser.first, testUser.last, testUser.email,
            testUser.password, testUser.authMethod, COOP
        )
        val savedUser = userService.createUser(request)
        testUser.uuid = savedUser.uuid
        val user = userService.find(testUser.uuid) ?: fail("User must not be null")
        assertThat(user.enabled).isFalse()
        return user
    }

    private fun generateSignupJson(): String {
        return """
            |{
            |  "signup_method" : "${testUser.authMethod}",
            |  "user_info" : {
            |       "first_name": "${testUser.first}",
            |       "last_name": "${testUser.last}",
            |       "email" : "${testUser.email}",
            |       "password" : "${testUser.password}"
            |   },
            |   "coop": "${testUser.coop}",
            |   "re_captcha_token" : "${testUser.reCaptchaToken}"
            |}
        """.trimMargin()
    }

    private fun verifySocialSignUp(authMethod: AuthMethod, token: String, email: String, coop: String = COOP) {
        suppose("User has obtained token on frontend and sends signup request") {
            val request =
                """
            |{
            |  "coop": "$coop",
            |  "signup_method" : "$authMethod",
            |  "user_info" : {
            |    "token" : "$token"
            |  },
            |   "re_captcha_token" : "${testUser.reCaptchaToken}"
            |}
            """.trimMargin()

            testContext.mvcResult = mockMvc.perform(
                post(pathSignup)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request)
            )
                .andExpect(status().isOk)
                .andReturn()
        }

        verify("The controller returned valid user") {
            val userResponse: UserResponse = objectMapper.readValue(testContext.mvcResult.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(email)
            assertThat(userResponse.role).isEqualTo(UserRole.USER.toString())
            assertThat(userResponse.uuid).isNotEmpty
            assertThat(userResponse.firstName).isNull()
            assertThat(userResponse.lastName).isNull()
            assertThat(userResponse.enabled).isTrue
            assertThat(userResponse.verified).isFalse()
        }

        verify("The user is stored in database") {
            val userInRepo = userService.find(email, COOP) ?: fail("User must not be null")
            assert(userInRepo.email == email)
            assert(userInRepo.role.id == UserRole.USER.id)
            assertThat(userInRepo.enabled).isTrue()
        }
    }

    private fun saveTestUser(): User {
        val user = createUser(testUser.email, testUser.authMethod, testUser.password, UUID.randomUUID())
        testUser.uuid = user.uuid
        return user
    }

    private class TestUser {
        var uuid: UUID = UUID.randomUUID()
        var email = "john@smith.com"
        var password = "abcdefgh"
        var authMethod = AuthMethod.EMAIL
        val first = "first"
        val last = "last"
        val coop = COOP
        var reCaptchaToken: String = "token"
    }

    private class TestContext {
        lateinit var mvcResult: MvcResult
        val token = "token"
        lateinit var socialEmail: String
        lateinit var mailConfirmationToken: UUID
        lateinit var user: User
    }
}
