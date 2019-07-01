package com.ampnet.userservice.controller

import com.ampnet.userservice.config.auth.TokenProvider
import com.ampnet.userservice.config.auth.UserPrincipal
import com.ampnet.userservice.controller.pojo.response.AuthTokenResponse
import com.ampnet.userservice.exception.ErrorResponse
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.SocialException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.SocialService
import com.ampnet.userservice.service.UserService
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ActiveProfiles("SocialMockConfig")
class AuthenticationControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var userService: UserService
    @Autowired
    private lateinit var tokenProvider: TokenProvider
    @Autowired
    private lateinit var socialService: SocialService

    private lateinit var result: MvcResult
    private lateinit var user: User

    private val tokenPath = "/token"
    private val regularTestUser = RegularTestUser()
    private val facebookTestUser = FacebookTestUser()
    private val googleTestUser = GoogleTestUser()

    @BeforeEach
    fun clearDatabase() {
        Mockito.reset(socialService)
        databaseCleanerService.deleteAllUsers()
    }

    @Test
    fun signInRegular() {
        suppose("User exists in database.") {
            user = createUser(regularTestUser.email, regularTestUser.authMethod, regularTestUser.password)
        }
        suppose("User mail is confirmed.") {
            val optionalUser = userRepository.findById(user.uuid)
            optionalUser.get().enabled = true
            user = userRepository.save(optionalUser.get())
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
            result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
        }
        verify("Token is valid.") {
            val response = objectMapper.readValue<AuthTokenResponse>(result.response.contentAsString)
            verifyTokenForUserData(response.token)
        }
    }

    @Test
    fun signInFacebook() {
        suppose("Social service is mocked to return valid Facebook user.") {
            Mockito.`when`(socialService.getFacebookEmail(facebookTestUser.fbToken))
                    .thenReturn(facebookTestUser.email)
        }
        suppose("Social user identified by Facebook exists in our database.") {
            user = createUser(facebookTestUser.email, facebookTestUser.authMethod)
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
            result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
        }
        verify("Token is valid.") {
            val response = objectMapper.readValue<AuthTokenResponse>(result.response.contentAsString)
            verifyTokenForUserData(response.token)
        }
    }

    @Test
    fun signInGoogle() {
        suppose("Social service is mocked to return valid Google user.") {
            Mockito.`when`(socialService.getGoogleEmail(googleTestUser.googleToken))
                    .thenReturn(googleTestUser.email)
        }
        suppose("Social user identified by Facebook exists in our database.") {
            user = createUser(googleTestUser.email, googleTestUser.authMethod)
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
            result = mockMvc.perform(
                    post(tokenPath)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
        }
        verify("Token is valid.") {
            val response = objectMapper.readValue<AuthTokenResponse>(result.response.contentAsString)
            verifyTokenForUserData(response.token)
        }
    }

    @Test
    fun signInWithInvalidCredentialsShouldFail() {
        suppose("User with email ${regularTestUser.email} exists in database.") {
            user = createUser(regularTestUser.email, regularTestUser.authMethod)
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
            result = mockMvc.perform(
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
            result = mockMvc.perform(
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
            user = createUser(googleTestUser.email, googleTestUser.authMethod)
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
            result = mockMvc.perform(
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
            user = createUser(facebookTestUser.email, facebookTestUser.authMethod)
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
            result = mockMvc.perform(
                post(tokenPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadGateway)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.REG_SOCIAL)
        }
    }

    private fun verifyTokenForUserData(token: String) {
        val tokenPrincipal = tokenProvider.parseToken(token)
        val storedUserPrincipal = UserPrincipal(user)
        assertThat(tokenPrincipal).isEqualTo(storedUserPrincipal)
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
