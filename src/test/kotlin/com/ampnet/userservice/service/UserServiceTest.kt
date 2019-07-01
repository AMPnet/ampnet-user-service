package com.ampnet.userservice.service

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.impl.UserServiceImpl
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.context.annotation.Import
import java.util.UUID

@Import(JsonConfig::class)
class UserServiceTest : JpaServiceTestBase() {

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        testContext = TestContext()

        val properties = ApplicationProperties()
        properties.mail.enabled = false
        testContext.applicationProperties = properties
    }

    @Test
    fun mustEnableNewAccountWithoutMailConfirmation() {
        suppose("Sending mail is disabled") {
            val properties = ApplicationProperties()
            properties.mail.enabled = false
            testContext.applicationProperties = properties
        }
        suppose("User has no account") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllMailTokens()
            testContext.email = "disabled@test.com"
        }
        suppose("User created new account") {
            val service = createUserService(testContext.applicationProperties)
            val userInfo = createUserInfo()
            val request = CreateUserServiceRequest(
                userInfo.webSessionUuid, testContext.email, "password", AuthMethod.EMAIL)
            testContext.user = service.createUser(request)
        }

        verify("Created user account is connected and enabled") {
            assertThat(testContext.user.userInfo.connected).isTrue()
            assertThat(testContext.user.enabled).isTrue()
        }
        verify("Sending mail confirmation was not called") {
            Mockito.verify(mailService, Mockito.never()).sendConfirmationMail(Mockito.anyString(), Mockito.anyString())
        }
    }

    @Test
    fun mustDisableNewAccountWithMailConfirmation() {
        suppose("Sending mail is disabled") {
            val properties = ApplicationProperties()
            properties.mail.enabled = true
            testContext.applicationProperties = properties
        }
        suppose("User has no account") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllMailTokens()
            testContext.email = "enabled@test.com"
        }
        suppose("User created new account") {
            val service = createUserService(testContext.applicationProperties)
            val userInfo = createUserInfo()
            val request = CreateUserServiceRequest(
                userInfo.webSessionUuid, testContext.email, "password", AuthMethod.EMAIL)
            testContext.user = service.createUser(request)
        }

        verify("Created user account is connected and disabled") {
            assertThat(testContext.user.userInfo.connected).isTrue()
            assertThat(testContext.user.enabled).isFalse()
        }
        verify("Sending mail confirmation was called") {
            val optionalMailToken = mailTokenRepository.findByUserUuid(testContext.user.uuid)
            assertThat(optionalMailToken).isPresent
            Mockito.verify(mailService, Mockito.times(1))
                    .sendConfirmationMail(testContext.user.email, optionalMailToken.get().token.toString())
        }
    }

    @Test
    fun mustBeAbleToChangeUserRoleToAdmin() {
        suppose("There is user with user role") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = roleRepository.getOne(UserRoleType.USER.id)
        }

        verify("Service can change user role to admin role") {
            val service = createUserService(testContext.applicationProperties)
            service.changeUserRole(testContext.user.uuid, UserRoleType.ADMIN)
        }
        verify("User has admin role") {
            val userWithNewRole = userRepository.findById(testContext.user.uuid)
            assertThat(userWithNewRole).isPresent
            assertThat(userWithNewRole.get().role.id).isEqualTo(UserRoleType.ADMIN.id)
        }
    }

    @Test
    fun mustBeAbleToChangeUserRoleToUser() {
        suppose("There is user with user role") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = roleRepository.getOne(UserRoleType.USER.id)
        }

        verify("Service can change user role to admin role") {
            val service = createUserService(testContext.applicationProperties)
            service.changeUserRole(testContext.user.uuid, UserRoleType.USER)
        }
        verify("User has admin role") {
            val userWithNewRole = userRepository.findById(testContext.user.uuid)
            assertThat(userWithNewRole).isPresent
            assertThat(userWithNewRole.get().role.id).isEqualTo(UserRoleType.USER.id)
        }
    }

    @Test
    fun mustThrowExceptionForChangeRoleOfNonExistingUser() {
        verify("Service will throw exception") {
            val service = createUserService(testContext.applicationProperties)
            val exception = assertThrows<InvalidRequestException> {
                service.changeUserRole(UUID.randomUUID(), UserRoleType.ADMIN)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING)
        }
    }

    private fun createUserService(properties: ApplicationProperties): UserService {
        return UserServiceImpl(userRepository, roleRepository, userInfoRepository, mailTokenRepository, mailService,
            passwordEncoder, properties)
    }

    private class TestContext {
        lateinit var applicationProperties: ApplicationProperties
        lateinit var email: String
        lateinit var user: User
    }
}
