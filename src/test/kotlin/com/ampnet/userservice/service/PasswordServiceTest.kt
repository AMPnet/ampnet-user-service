package com.ampnet.userservice.service

import com.ampnet.userservice.COOP
import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.persistence.model.ForgotPasswordToken
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.impl.PasswordServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime
import java.util.UUID

class PasswordServiceTest : JpaServiceTestBase() {

    private val service: PasswordService by lazy {
        val properties = ApplicationProperties()
        PasswordServiceImpl(
            userRepository, forgotPasswordTokenRepository, coopRepository, passwordEncoder, mailService, properties
        )
    }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllUsers()
        testContext = TestContext()
    }

    @Test
    fun mustThrowExceptionForMissingUser() {
        verify("Service will throw exception for missing user") {
            val exception = assertThrows<InvalidRequestException> {
                service.changePassword(UUID.randomUUID(), "oldPassword", "newPassword")
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_JWT_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionForUpdatingPasswordWithInvalidAuthMethod() {
        suppose("User is created with Google auth method") {
            testContext.user = createUser("user@dsm.cl", authMethod = AuthMethod.GOOGLE)
        }

        verify("Service will throw exception if user with Google auth method tries to change password") {
            val exception = assertThrows<InvalidRequestException> {
                service.changePassword(testContext.user.uuid, "oldPassword", "newPassword")
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.AUTH_INVALID_LOGIN_METHOD)
        }
    }

    @Test
    fun mustThrowExceptionForUpdatingPasswordWithInvalidOldPassword() {
        suppose("User is created") {
            testContext.password = "oldPassword"
            testContext.user = createUser("user@dsm.cl", password = testContext.password)
        }

        verify("Service will throw exception if user with Google auth method tries to change password") {
            val exception = assertThrows<InvalidRequestException> {
                service.changePassword(testContext.user.uuid, testContext.password, "newPassword")
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_DIFFERENT_PASSWORD)
        }
    }

    @Test
    fun mustNotGenerateForgotPasswordTokenForNonExistingEmail() {
        verify("Service will return false for generating forgot token with non existing email") {
            val created = service.generateForgotPasswordToken("non-existing@mail.com", COOP)
            assertThat(created).isFalse()
        }
    }

    @Test
    fun mustThrowExceptionForChangePasswordWithNonExistingToken() {
        suppose("There is no forgot password token") {
            databaseCleanerService.deleteAllForgotPasswordTokens()
        }

        verify("Service will throw exception for change password with non existing token") {
            val exception = assertThrows<ResourceNotFoundException> {
                service.changePasswordWithToken(UUID.randomUUID(), "newPassword")
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.AUTH_FORGOT_TOKEN_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionForChangePasswordWithGoogleAuthMethod() {
        suppose("User is created with Google auth method") {
            testContext.user = createUser("user@google.com", authMethod = AuthMethod.GOOGLE)
        }

        verify("Service will throw exception if user with Google auth method tries to change password") {
            val exception = assertThrows<InvalidRequestException> {
                service.generateForgotPasswordToken(testContext.user.email, COOP)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.AUTH_INVALID_LOGIN_METHOD)
        }
    }

    @Test
    fun mustThrowExceptionForChangePasswordWithExpiredToken() {
        suppose("User created forgot password token") {
            val user = createUser("forgot@password.com")
            testContext.forgotPasswordToken = createForgotPasswordToken(user, ZonedDateTime.now().minusHours(2))
        }

        verify("Service will throw exception for changing password with expired token") {
            val exception = assertThrows<InvalidRequestException> {
                service.changePasswordWithToken(testContext.forgotPasswordToken.token, "newPassword")
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.AUTH_FORGOT_TOKEN_EXPIRED)
        }
    }

    private fun createForgotPasswordToken(
        user: User,
        createdAt: ZonedDateTime = ZonedDateTime.now()
    ): ForgotPasswordToken {
        val token = ForgotPasswordToken(0, user, UUID.randomUUID(), createdAt)
        return forgotPasswordTokenRepository.save(token)
    }

    private class TestContext {
        lateinit var user: User
        lateinit var password: String
        lateinit var forgotPasswordToken: ForgotPasswordToken
    }
}
