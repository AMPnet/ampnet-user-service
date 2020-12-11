package com.ampnet.userservice.service

import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.impl.UserMailServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class UserMailServiceTest : JpaServiceTestBase() {

    private lateinit var testContext: TestContext
    private val service: UserMailServiceImpl by lazy {
        UserMailServiceImpl(mailTokenRepository, mailService)
    }

    @BeforeEach
    fun init() {
        Mockito.reset(mailService)
        testContext = TestContext()
    }

    @Test
    fun mustNotSendMailConfirmationForNonEmailAuthMethod() {
        suppose("User register using Google") {
            testContext.user = createUser("google@mail.com", authMethod = AuthMethod.GOOGLE)
        }

        verify("Service will not send email confirmation for Google auth method") {
            service.sendMailConfirmation(testContext.user)
            Mockito.verifyNoInteractions(mailService)
        }
    }

    @Test
    fun mustNotResendMailConfirmationForNonEmailAuthMethod() {
        suppose("User register using Google") {
            testContext.user = createUser("google@mail.com", authMethod = AuthMethod.GOOGLE)
        }

        verify("Service will not resend email confirmation for Google auth method") {
            service.resendConfirmationMail(testContext.user)
            Mockito.verifyNoInteractions(mailService)
        }
    }

    private class TestContext {
        lateinit var user: User
    }
}
