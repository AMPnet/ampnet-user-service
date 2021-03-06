package com.ampnet.userservice.service

import com.ampnet.userservice.COOP
import com.ampnet.userservice.amqp.mailservice.UserDataWithToken
import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.service.impl.UserMailServiceImpl
import com.ampnet.userservice.service.impl.UserServiceImpl
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.UUID

class UserServiceTest : JpaServiceTestBase() {

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        databaseCleanerService.deleteAllCoop()
        testContext = TestContext()

        val properties = ApplicationProperties()
        properties.mail.confirmationNeeded = false
        testContext.applicationProperties = properties
    }

    @Test
    fun mustEnableNewAccountWithoutMailConfirmation() {
        suppose("Sending mail is disabled") {
            val properties = ApplicationProperties()
            properties.mail.confirmationNeeded = false
            testContext.applicationProperties = properties
        }
        suppose("User has no account") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllMailTokens()
            testContext.email = "disabled@test.com"
        }
        suppose("There is a coop") {
            createCoop()
        }
        suppose("User created new account") {
            val service = createUserService(testContext.applicationProperties)
            val request = CreateUserServiceRequest(
                "first", "last", testContext.email,
                "password", AuthMethod.EMAIL, COOP
            )
            testContext.user = service.createUser(request)
        }

        verify("Created user account is connected and enabled") {
            assertThat(testContext.user.enabled).isTrue()
        }
        verify("Sending mail confirmation was not called") {
            Mockito.verify(mailService, Mockito.never()).sendConfirmationMail(
                UserDataWithToken(testContext.user, Mockito.anyString())
            )
        }
    }

    @Test
    fun mustDisableNewAccountWithMailConfirmation() {
        suppose("Sending mail is disabled") {
            val properties = ApplicationProperties()
            properties.mail.confirmationNeeded = true
            testContext.applicationProperties = properties
        }
        suppose("User has no account") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllMailTokens()
            testContext.email = "enabled@test.com"
        }
        suppose("There is a coop") {
            createCoop()
        }
        suppose("User created new account") {
            val service = createUserService(testContext.applicationProperties)
            val request = CreateUserServiceRequest(
                "first", "last", testContext.email,
                "password", AuthMethod.EMAIL, COOP
            )
            testContext.user = service.createUser(request)
        }

        verify("Created user account is connected and disabled") {
            assertThat(testContext.user.enabled).isFalse()
        }
        verify("Sending mail confirmation was called") {
            val optionalMailToken = mailTokenRepository.findByUserUuid(testContext.user.uuid)
            assertThat(optionalMailToken).isPresent
            Mockito.verify(mailService, Mockito.times(1))
                .sendConfirmationMail(UserDataWithToken(testContext.user, optionalMailToken.get().token))
        }
    }

    @Test
    fun mustThrowExceptionIfUserIsMissing() {
        suppose("User info exists") {
            testContext.userInfo = createUserInfo()
        }
        verify("Service will throw exception that user is missing") {
            val service = createUserService(testContext.applicationProperties)
            val exception = assertThrows<ResourceNotFoundException> {
                service.connectUserInfo(UUID.randomUUID(), testContext.userInfo.sessionId)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_JWT_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfUserInfoIsMissing() {
        suppose("User created account") {
            testContext.user = createUser("my@email.com")
        }
        verify("Service will throw exception that user is missing") {
            val service = createUserService(testContext.applicationProperties)
            val exception = assertThrows<ResourceNotFoundException> {
                service.connectUserInfo(testContext.user.uuid, UUID.randomUUID().toString())
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.REG_INCOMPLETE)
        }
    }

    @Test
    fun mustSetFirstUserAsAdmin() {
        suppose("First admin rule is enabled") {
            val properties = ApplicationProperties()
            properties.user.firstAdmin = true
            testContext.applicationProperties = properties
        }
        suppose("There are coops") {
            createCoop()
            testContext.newCoop = createCoop("new-coop").identifier
        }
        suppose("There is user in another coop") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllMailTokens()
            createUser("new-coop@mail.com", coop = testContext.newCoop)
        }
        suppose("User created new account") {
            val service = createUserService(testContext.applicationProperties)
            val request = CreateUserServiceRequest(
                "first", "last", "admin@email.com",
                "password", AuthMethod.EMAIL, COOP
            )
            testContext.user = service.createUser(request)
        }

        verify("Created user has admin role") {
            assertThat(testContext.user.role.name).isEqualTo(UserRole.ADMIN.name)
        }
    }

    @Test
    fun secondUserMustNotBeAdmin() {
        suppose("First admin rule is enabled") {
            val properties = ApplicationProperties()
            properties.user.firstAdmin = true
            testContext.applicationProperties = properties
        }
        suppose("There is a coop") {
            createCoop()
        }
        suppose("There is one user") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllMailTokens()
            createUser("first@email.com")
        }
        suppose("Second user created account") {
            val service = createUserService(testContext.applicationProperties)
            val request = CreateUserServiceRequest(
                "first", "last", "admin@email.com",
                "password", AuthMethod.EMAIL, COOP
            )
            testContext.user = service.createUser(request)
        }

        verify("Second user is not admin") {
            assertThat(testContext.user.role.name).isEqualTo(UserRole.USER.name)
        }
    }

    @Test
    fun mustNotSetFirstUserAsUser() {
        suppose("First admin rule is disabled") {
            val properties = ApplicationProperties()
            properties.user.firstAdmin = false
            testContext.applicationProperties = properties
        }
        suppose("There is a coop") {
            createCoop()
        }
        suppose("There are no users") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllMailTokens()
        }
        suppose("User created new account") {
            val service = createUserService(testContext.applicationProperties)
            val request = CreateUserServiceRequest(
                "first", "last", "user@email.com",
                "password", AuthMethod.EMAIL, COOP
            )
            testContext.user = service.createUser(request)
        }

        verify("Created user has user role") {
            assertThat(testContext.user.role.name).isEqualTo(UserRole.USER.name)
        }
    }

    @Test
    fun mustCreateUserWithExistingEmailInAnotherCoop() {
        suppose("There are two cops") {
            createCoop(COOP)
            testContext.newCoop = createCoop("new-cop").identifier
        }
        suppose("User created account") {
            databaseCleanerService.deleteAllUsers()
            testContext.email = "user@double.email"
            testContext.user = createUser(testContext.email)
        }

        verify("User can create account with the same email in another coop") {
            val service = createUserService(testContext.applicationProperties)
            val request = CreateUserServiceRequest(
                "first", "last", testContext.email,
                "password", AuthMethod.EMAIL, testContext.newCoop
            )
            testContext.user = service.createUser(request)
        }
        verify("User is in two coops") {
            val user = userRepository.findByCoopAndEmail(testContext.user.coop, testContext.email)
            assertThat(user).isPresent
            val userWithSameMail = userRepository.findByCoopAndEmail(COOP, testContext.email)
            assertThat(userWithSameMail).isPresent
        }
    }

    @Test
    fun mustThrowExceptionIfSignUpIsDisabledForCoop() {
        suppose("There is a coop with signup disabled") {
            testContext.coop = createCoop(disableSignUp = true)
        }

        verify("Service will throw exception for signUp disabled") {
            val service = createUserService(testContext.applicationProperties)
            val request = CreateUserServiceRequest(
                "first", "last", "email",
                "password", AuthMethod.EMAIL, testContext.coop.identifier
            )
            val exception = assertThrows<InvalidRequestException> {
                service.createUser(request)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.REG_SIGNUP_DISABLED)
        }
    }

    @Test
    fun mustDisconnectOldUserInfo() {
        suppose("User has user info") {
            databaseCleanerService.deleteAllUsers()
            databaseCleanerService.deleteAllUserInfos()
            testContext.user = createUser("connected@user.com")
            testContext.userInfo = createUserInfo()
            setUserInfo(testContext.user, testContext.userInfo.uuid)
        }

        verify("User can set new user info") {
            val newUserInfo = createUserInfo()
            val service = createUserService(testContext.applicationProperties)
            val user = service.connectUserInfo(testContext.user.uuid, newUserInfo.sessionId)
            assertThat(user.userInfoUuid).isEqualTo(newUserInfo.uuid)
        }
        verify("Old user info is disconnected") {
            val userInfo = userInfoRepository.findById(testContext.userInfo.uuid).get()
            assertThat(userInfo.connected).isFalse
            assertThat(userInfo.deactivated).isTrue
        }
    }

    private fun createUserService(properties: ApplicationProperties): UserService {
        val userMailService = UserMailServiceImpl(mailTokenRepository, mailService)
        return UserServiceImpl(
            userRepository, userInfoRepository, coopRepository, userMailService, passwordEncoder, properties
        )
    }

    private class TestContext {
        lateinit var applicationProperties: ApplicationProperties
        lateinit var email: String
        lateinit var user: User
        lateinit var newCoop: String
        lateinit var coop: Coop
        lateinit var userInfo: UserInfo
    }
}
