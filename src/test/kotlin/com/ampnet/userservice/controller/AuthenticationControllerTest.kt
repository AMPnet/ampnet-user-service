package com.ampnet.userservice.controller

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.auth.TokenProvider
import com.ampnet.userservice.config.auth.UserPrincipal
import com.ampnet.userservice.controller.pojo.request.RefreshTokenRequest
import com.ampnet.userservice.controller.pojo.response.AccessRefreshTokenResponse
import com.ampnet.userservice.exception.ErrorResponse
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.SocialException
import com.ampnet.userservice.persistence.model.RefreshToken
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.RefreshTokenRepository
import com.ampnet.userservice.service.SocialService
import com.ampnet.userservice.service.UserService
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

@ActiveProfiles("SocialMockConfig")
class AuthenticationControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var tokenProvider: TokenProvider
    @Autowired
    private lateinit var socialService: SocialService
    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private val tokenPath = "/token"
    private val tokenRefreshPath = "/token/refresh"
    private val regularTestUser = RegularTestUser()
    private val facebookTestUser = FacebookTestUser()
    private val googleTestUser = GoogleTestUser()

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        Mockito.reset(socialService)
        databaseCleanerService.deleteAllUsers()
        databaseCleanerService.deleteAllRefreshTokens()
        testContext = TestContext()
    }

    @Test
    fun signInRegular() {
        suppose("User exists in database.") {
            testContext.user = createUser(regularTestUser.email, regularTestUser.authMethod, regularTestUser.password)
        }
        suppose("User mail is confirmed.") {
            val optionalUser = userRepository.findById(testContext.user.uuid)
            optionalUser.get().enabled = true
            testContext.user = userRepository.save(optionalUser.get())
        }
        verify("User can fetch token with valid credentials.") {
            val requestBody = """
                |{
                |  "login_method" : "${regularTestUser.authMethod}",
                |  "credentials" : {
                |    "email" : "${regularTestUser.email}",
                |    "password" : "${regularTestUser.password}"
                |  }
                |}
            """.trimMargin()
            val result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            testContext.tokenResponse = objectMapper.readValue(result.response.contentAsString)
        }
        verify("Access and refresh token are valid.") {
            verifyAccessRefreshTokenResponse(testContext.tokenResponse)
        }
        verify("Refresh token is generated") {
            val optionalRefreshToken = refreshTokenRepository.findByUserUuid(testContext.user.uuid)
            assertThat(optionalRefreshToken).isPresent
        }
    }

    @Test
    fun signInFacebook() {
        suppose("Social service is mocked to return valid Facebook user.") {
            Mockito.`when`(socialService.getFacebookEmail(facebookTestUser.fbToken))
                    .thenReturn(facebookTestUser.email)
        }
        suppose("Social user identified by Facebook exists in our database.") {
            testContext.user = createUser(facebookTestUser.email, facebookTestUser.authMethod)
        }
        verify("User can fetch token with valid credentials.") {
            val requestBody = """
                |{
                |  "login_method" : "${facebookTestUser.authMethod}",
                |  "credentials" : {
                |    "token" : "${facebookTestUser.fbToken}"
                |  }
                |}
            """.trimMargin()
            val result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            testContext.tokenResponse = objectMapper.readValue(result.response.contentAsString)
        }
        verify("Access and refresh token are valid.") {
            verifyAccessRefreshTokenResponse(testContext.tokenResponse)
        }
        verify("Refresh token is generated") {
            val optionalRefreshToken = refreshTokenRepository.findByUserUuid(testContext.user.uuid)
            assertThat(optionalRefreshToken).isPresent
        }
    }

    @Test
    fun signInGoogle() {
        suppose("Social service is mocked to return valid Google user.") {
            Mockito.`when`(socialService.getGoogleEmail(googleTestUser.googleToken))
                    .thenReturn(googleTestUser.email)
        }
        suppose("Social user identified by Facebook exists in our database.") {
            testContext.user = createUser(googleTestUser.email, googleTestUser.authMethod)
        }
        verify("User can fetch token with valid credentials.") {
            val requestBody = """
                |{
                |  "login_method" : "${googleTestUser.authMethod}",
                |  "credentials" : {
                |    "token" : "${googleTestUser.googleToken}"
                |  }
                |}
            """.trimMargin()
            val result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            testContext.tokenResponse = objectMapper.readValue(result.response.contentAsString)
        }
        verify("Access and refresh token are valid.") {
            verifyAccessRefreshTokenResponse(testContext.tokenResponse)
        }
        verify("Refresh token is generated") {
            val optionalRefreshToken = refreshTokenRepository.findByUserUuid(testContext.user.uuid)
            assertThat(optionalRefreshToken).isPresent
        }
    }

    @Test
    fun signInWithInvalidCredentialsShouldFail() {
        suppose("User with email ${regularTestUser.email} exists in database.") {
            testContext.user = createUser(regularTestUser.email, regularTestUser.authMethod)
        }
        verify("User cannot fetch token with invalid credentials") {
            val requestBody = """
                |{
                |  "login_method" : "${regularTestUser.authMethod}",
                |  "credentials" : {
                |    "email" : "${regularTestUser.email}",
                |    "password" : "wrong-password"
                |  }
                |}
            """.trimMargin()
            mockMvc.perform(
                    post(tokenPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                    .andExpect(status().isUnauthorized)
        }
    }

    @Test
    fun signInWithNonExistingUserShouldFail() {
        suppose("User with email ${regularTestUser.email} does not exist in database.") {
            val user = userService.find(regularTestUser.email)
            assertThat(user).isNull()
        }
        verify("User cannot fetch token without signing up first.") {
            val requestBody = """
                |{
                |  "login_method" : "${regularTestUser.authMethod}",
                |  "credentials" : {
                |    "email" : "${regularTestUser.email}",
                |    "password" : "${regularTestUser.password}"
                |  }
                |}
            """.trimMargin()
            val result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            val error = objectMapper.readValue<ErrorResponse>(result.response.contentAsString)
            val expectedErrorCode = getResponseErrorCode(ErrorCode.USER_MISSING)
            assert(error.errCode == expectedErrorCode)
        }
    }

    @Test
    fun signInWithInvalidLoginMethodShouldFail() {
        suppose("User exists in database, created by regular registration.") {
            createUser(googleTestUser.email)
        }
        suppose("Social service is mocked to return google user with same email as user registered in regular way.") {
            Mockito.`when`(socialService.getGoogleEmail(googleTestUser.googleToken))
                    .thenReturn(googleTestUser.email)
        }
        verify("The user cannot login using social method.") {
            val requestBody = """
                |{
                |  "login_method" : "${googleTestUser.authMethod}",
                |  "credentials" : {
                |      "token" : "${googleTestUser.googleToken}"
                |  }
                |}
            """.trimMargin()
            val result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            val errorResponse = objectMapper.readValue<ErrorResponse>(result.response.contentAsString)
            val expectedErrorCode = getResponseErrorCode(ErrorCode.AUTH_INVALID_LOGIN_METHOD)
            assert(errorResponse.errCode == expectedErrorCode)
        }
    }

    @Test
    fun signInWithGoogleException() {
        suppose("Social service is mocked to return valid Google user.") {
            Mockito.`when`(socialService.getGoogleEmail(googleTestUser.googleToken))
                .thenThrow(SocialException(ErrorCode.REG_SOCIAL, "Google"))
        }
        suppose("Social user identified by Facebook exists in our database.") {
            testContext.user = createUser(googleTestUser.email, googleTestUser.authMethod)
        }
        verify("User can fetch token with valid credentials.") {
            val requestBody = """
                |{
                |  "login_method" : "${googleTestUser.authMethod}",
                |  "credentials" : {
                |    "token" : "${googleTestUser.googleToken}"
                |  }
                |}
            """.trimMargin()
            val result = mockMvc.perform(
                post(tokenPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadGateway)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_SOCIAL)
        }
    }

    @Test
    fun signInWithFacebookException() {
        suppose("Social service is mocked to return valid Google user.") {
            Mockito.`when`(socialService.getFacebookEmail(facebookTestUser.fbToken))
                .thenThrow(SocialException(ErrorCode.REG_SOCIAL, "Facebook"))
        }
        suppose("Social user identified by Facebook exists in our database.") {
            testContext.user = createUser(facebookTestUser.email, facebookTestUser.authMethod)
        }

        verify("User can fetch token with valid credentials.") {
            val requestBody = """
                |{
                |  "login_method" : "${facebookTestUser.authMethod}",
                |  "credentials" : {
                |    "token" : "${facebookTestUser.fbToken}"
                |  }
                |}
            """.trimMargin()
            val result = mockMvc.perform(
                post(tokenPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadGateway)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_SOCIAL)
        }
    }

    @Test
    fun mustBeAbleToGetAccessTokenWithRefreshToken() {
        suppose("Refresh token exists") {
            testContext.user = createUser(regularTestUser.email, regularTestUser.authMethod)
            testContext.refreshToken = createRefreshToken(testContext.user, ZonedDateTime.now().minusHours(1))
        }

        verify("User can get access token using refresh token") {
            val request = RefreshTokenRequest(testContext.refreshToken.token)
            val result = mockMvc.perform(post(tokenRefreshPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk)
                .andReturn()

            val response = objectMapper.readValue<AccessRefreshTokenResponse>(result.response.contentAsString)
            verifyTokenForUserData(response.accessToken)
            assertThat(response.expiresIn).isEqualTo(applicationProperties.jwt.accessTokenValidity)
            assertThat(response.refreshToken).isEqualTo(testContext.refreshToken.token)
            assertThat(response.refreshTokenExpiresIn).isLessThan(applicationProperties.jwt.refreshTokenValidity)
        }
    }

    @Test
    fun mustNotBeAbleToGetAccessTokenWithExpiredRefreshToken() {
        suppose("Refresh token expired") {
            testContext.user = createUser(regularTestUser.email, regularTestUser.authMethod)
            val createdAt = ZonedDateTime.now().minusSeconds(applicationProperties.jwt.refreshTokenValidity + 1000L)
            testContext.refreshToken = createRefreshToken(testContext.user, createdAt)
        }

        verify("User will get bad request response") {
            val request = RefreshTokenRequest(testContext.refreshToken.token)
            mockMvc.perform(post(tokenRefreshPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun mustNotBeAbleToGetAccessTokenWithNonExistingRefreshToken() {
        verify("User will get bad request response") {
            val request = RefreshTokenRequest("non-existing-refresh-token")
            mockMvc.perform(post(tokenRefreshPath)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    fun mustBeAbleToLogoutUser() {
        suppose("Refresh token exists") {
            testContext.user = createUser(regularTestUser.email, regularTestUser.authMethod)
            testContext.refreshToken = createRefreshToken(testContext.user)
        }
        suppose("User is authenticated") {
            val userPrincipal = UserPrincipal(testContext.user)
            val token = UsernamePasswordAuthenticationToken(userPrincipal, "", testContext.user.getAuthorities())
            val securityContext = SecurityContextHolder.getContext()
            securityContext.authentication = token
        }

        verify("User can logout") {
            mockMvc.perform(post("/logout"))
                .andExpect(status().isOk)
        }
        verify("Refresh token is deleted") {
            val optionalRefreshToken = refreshTokenRepository.findById(testContext.refreshToken.id)
            assertThat(optionalRefreshToken).isNotPresent
        }
    }

    private fun verifyAccessRefreshTokenResponse(response: AccessRefreshTokenResponse) {
        verifyTokenForUserData(response.accessToken)
        assertThat(response.expiresIn).isEqualTo(applicationProperties.jwt.accessTokenValidity)
        assertThat(response.refreshToken).isNotNull()
        assertThat(response.refreshTokenExpiresIn).isEqualTo(applicationProperties.jwt.refreshTokenValidity)
    }

    private fun verifyTokenForUserData(token: String) {
        val tokenPrincipal = tokenProvider.parseToken(token)
        val storedUserPrincipal = UserPrincipal(testContext.user)
        assertThat(tokenPrincipal).isEqualTo(storedUserPrincipal)
    }

    private fun createRefreshToken(user: User, createdAt: ZonedDateTime = ZonedDateTime.now()): RefreshToken {
        val refreshToken = RefreshToken(0, user, "9asdf90asf90asf9asfis90fkas90fkas", createdAt)
        return refreshTokenRepository.save(refreshToken)
    }

    private class TestContext {
        lateinit var refreshToken: RefreshToken
        lateinit var tokenResponse: AccessRefreshTokenResponse
        lateinit var user: User
    }

    private class RegularTestUser {
        val email = "john@smith.com"
        val password = "Password175!"
        val authMethod = AuthMethod.EMAIL
    }

    private class FacebookTestUser {
        val email = "john@smith.com"
        val fbToken = "token"
        val authMethod = AuthMethod.FACEBOOK
    }

    private class GoogleTestUser {
        val email = "john@smith.com"
        val googleToken = "token"
        val authMethod = AuthMethod.GOOGLE
    }
}
