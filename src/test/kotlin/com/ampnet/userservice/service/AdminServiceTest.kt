package com.ampnet.userservice.service

import com.ampnet.userservice.COOP
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.impl.AdminServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AdminServiceTest : JpaServiceTestBase() {

    private val service: AdminService by lazy { AdminServiceImpl(userRepository, userInfoRepository) }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        testContext = TestContext()
    }

    /* Find by user role */
    @Test
    fun mustBeAbleToGetPlatformManagers() {
        suppose("There is an admin user") {
            testContext.user = createUser("admin@test.com", "Invited", "User")
            service.changeUserRole(COOP, testContext.user.uuid, UserRole.ADMIN)
        }
        suppose("There is a platform manager user") {
            testContext.secondUser = createUser("plm@test.com", "Plm", "User")
            service.changeUserRole(COOP, testContext.secondUser.uuid, UserRole.PLATFORM_MANAGER)
        }
        suppose("There is a user") {
            createUser("user@test.com", "Invited", "User")
        }

        verify("Service will return platform managers") {
            val platformManagers = service.findByRoles(COOP, listOf(UserRole.PLATFORM_MANAGER, UserRole.ADMIN))
            assertThat(platformManagers).hasSize(2)
            assertThat(platformManagers.map { it.uuid })
                .containsAll(listOf(testContext.user.uuid, testContext.secondUser.uuid))
        }
    }

    @Test
    fun mustBeAbleToGetTokenIssuers() {
        suppose("There is an admin user") {
            testContext.user = createUser("admin@test.com", "Invited", "User")
            service.changeUserRole(COOP, testContext.user.uuid, UserRole.ADMIN)
        }
        suppose("There is a token issuer user") {
            testContext.secondUser = createUser("tki@test.com", "Tki", "User")
            service.changeUserRole(COOP, testContext.secondUser.uuid, UserRole.TOKEN_ISSUER)
        }
        suppose("There is a user") {
            createUser("user@test.com", "Invited", "User")
        }

        verify("Service will return token issuers") {
            val tokenIssuers = service.findByRoles(COOP, listOf(UserRole.ADMIN, UserRole.TOKEN_ISSUER))
            assertThat(tokenIssuers).hasSize(2)
            assertThat(tokenIssuers.map { it.uuid })
                .containsAll(listOf(testContext.user.uuid, testContext.secondUser.uuid))
        }
    }

    /* Change user roles */
    @Test
    fun mustBeAbleToChangeUserRoleToAdmin() {
        suppose("There is user with user role") {
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = UserRole.USER
        }
        suppose("There is a user with platform manager role") {
            testContext.secondUser = createUser("platform@test.com")
            testContext.secondUser.role = UserRole.PLATFORM_MANAGER
        }
        suppose("There is a user with token issuer role") {
            testContext.thirdUser = createUser("token@test.com")
            testContext.thirdUser.role = UserRole.TOKEN_ISSUER
        }

        verify("Service can change user role to admin role") {
            service.changeUserRole(COOP, testContext.user.uuid, UserRole.ADMIN)
        }
        verify("User has admin role") {
            verifyUserRole(testContext.user.uuid, UserRole.ADMIN)
        }
        verify("Old platform manger has user role") {
            verifyUserRole(testContext.secondUser.uuid, UserRole.USER)
        }
        verify("Old token issuer has user role") {
            verifyUserRole(testContext.thirdUser.uuid, UserRole.USER)
        }
    }

    @Test
    fun mustNotChangeUserRoleToAdminForAnotherCoop() {
        suppose("There is user with user role") {
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = UserRole.USER
        }

        verify("Service cannot change user role to admin role") {
            val exception = assertThrows<InvalidRequestException> {
                service.changeUserRole("another-coop", testContext.user.uuid, UserRole.ADMIN)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING)
        }
    }

    @Test
    fun mustNotBeAbleToChangeUserRoleToUser() {
        suppose("There is user with platform manager role") {
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = UserRole.PLATFORM_MANAGER
        }

        verify("Service cannot change user role to user role") {
            val exception = assertThrows<InvalidRequestException> {
                service.changeUserRole(COOP, testContext.user.uuid, UserRole.USER)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.INT_REQUEST)
        }
    }

    @Test
    fun mustBeAbleToChangeUserRoleToTokenIssuer() {
        suppose("There is user with user role") {
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = UserRole.USER
        }
        suppose("There is a user with token issuer role") {
            testContext.secondUser = createUser("token@test.com")
            testContext.user.role = UserRole.TOKEN_ISSUER
        }

        verify("Service can change user role to token issuer role") {
            service.changeUserRole(COOP, testContext.user.uuid, UserRole.TOKEN_ISSUER)
        }
        verify("User has admin role") {
            verifyUserRole(testContext.user.uuid, UserRole.TOKEN_ISSUER)
        }
        verify("Old token issuer has user role") {
            verifyUserRole(testContext.secondUser.uuid, UserRole.USER)
        }
    }

    @Test
    fun mustNotChangeUserToTokenIssuerForAnotherCoop() {
        suppose("There is user with user role") {
            testContext.user = createUser("admin@test.com", "Invited", "User")
            testContext.user.role = UserRole.ADMIN
        }

        verify("Service cannot change user role to token issuer role") {
            val exception = assertThrows<InvalidRequestException> {
                service.changeUserRole("other-coop", testContext.user.uuid, UserRole.TOKEN_ISSUER)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING)
        }
    }

    @Test
    fun mustBeAbleToChangeUserRoleToPlatformManager() {
        suppose("There is user with user role") {
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = UserRole.USER
        }
        suppose("There is a user with platform manager role") {
            testContext.secondUser = createUser("platform@test.com")
            testContext.user.role = UserRole.PLATFORM_MANAGER
        }

        verify("Service can change user role to platform manager role") {
            service.changeUserRole(COOP, testContext.user.uuid, UserRole.PLATFORM_MANAGER)
        }
        verify("User has admin role") {
            verifyUserRole(testContext.user.uuid, UserRole.PLATFORM_MANAGER)
        }
        verify("Old platform manager has user role") {
            verifyUserRole(testContext.secondUser.uuid, UserRole.USER)
        }
    }

    @Test
    fun mustNotChangeUserRoleToPlatformMangerForAnotherCoop() {
        suppose("There is user with user role") {
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = UserRole.USER
        }

        verify("Service cannot change user role to token issuer role") {
            val exception = assertThrows<InvalidRequestException> {
                service.changeUserRole("other-coop", testContext.user.uuid, UserRole.PLATFORM_MANAGER)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING)
        }
    }

    @Test
    fun mustRemoveOldAdminOnTransferAdminRole() {
        suppose("There is user with user role") {
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = UserRole.USER
        }
        suppose("There is a user with admin role") {
            testContext.secondUser = createUser("admin@test.com")
            testContext.secondUser.role = UserRole.ADMIN
        }

        verify("Service can change user role to admin role") {
            service.changeUserRole(COOP, testContext.user.uuid, UserRole.ADMIN)
        }
        verify("User has admin role") {
            verifyUserRole(testContext.user.uuid, UserRole.ADMIN)
        }
        verify("Old admin has user role") {
            verifyUserRole(testContext.secondUser.uuid, UserRole.USER)
        }
    }

    @Test
    fun mustThrowExceptionForChangeRoleOfNonExistingUser() {
        verify("Service will throw exception") {
            val exception = assertThrows<InvalidRequestException> {
                service.changeUserRole(COOP, UUID.randomUUID(), UserRole.ADMIN)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING)
        }
    }

    private fun verifyUserRole(userUuid: UUID, role: UserRole) {
        val userWithNewRole = userRepository.findById(userUuid)
        assertThat(userWithNewRole).isPresent
        assertThat(userWithNewRole.get().role.id).isEqualTo(role.id)
    }

    private class TestContext {
        lateinit var user: User
        lateinit var secondUser: User
        lateinit var thirdUser: User
    }
}
