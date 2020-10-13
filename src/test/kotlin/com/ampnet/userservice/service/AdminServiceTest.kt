package com.ampnet.userservice.service

import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.service.impl.AdminServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.annotation.Import
import java.util.UUID

@Import(JsonConfig::class)
class AdminServiceTest : JpaServiceTestBase() {

    private val service: AdminService by lazy {
        AdminServiceImpl(userRepository, userInfoRepository, roleRepository, passwordEncoder)
    }

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestContext() {
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToChangeUserRoleToAdmin() {
        suppose("There is user with user role") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = roleRepository.getOne(UserRoleType.USER.id)
        }

        verify("Service can change user role to admin role") {
            service.changeUserRole(testContext.user.uuid, UserRoleType.ADMIN)
        }
        verify("User has admin role") {
            val userWithNewRole = userRepository.findById(testContext.user.uuid)
            assertThat(userWithNewRole).isPresent
            assertThat(userWithNewRole.get().role.id).isEqualTo(UserRoleType.ADMIN.id)
        }
    }

    @Test
    fun mustBeAbleToGetPlatformManagers() {
        suppose("There is an admin user") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser("admin@test.com", "Invited", "User")
            service.changeUserRole(testContext.user.uuid, UserRoleType.ADMIN)
        }
        suppose("There is a platform manager user") {
            testContext.secondUser = createUser("plm@test.com", "Plm", "User")
            service.changeUserRole(testContext.secondUser.uuid, UserRoleType.PLATFORM_MANAGER)
        }
        suppose("There is a user") {
            createUser("user@test.com", "Invited", "User")
        }

        verify("Service will return platform managers") {
            val platformManagers = service.findByRoles(listOf(UserRoleType.PLATFORM_MANAGER, UserRoleType.ADMIN))
            assertThat(platformManagers).hasSize(2)
            assertThat(platformManagers.map { it.uuid })
                .containsAll(listOf(testContext.user.uuid, testContext.secondUser.uuid))
        }
    }

    @Test
    fun mustBeAbleToGetTokenIssuers() {
        suppose("There is an admin user") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser("admin@test.com", "Invited", "User")
            testContext.userInfo = createUserInfo()
            setUserInfo(testContext.user, testContext.userInfo.id)
            service.changeUserRole(testContext.user.uuid, UserRoleType.ADMIN)
        }
        suppose("There is a token issuer user") {
            testContext.secondUser = createUser("tki@test.com", "Tki", "User")
            service.changeUserRole(testContext.secondUser.uuid, UserRoleType.TOKEN_ISSUER)
        }
        suppose("There is a user") {
            createUser("user@test.com", "Invited", "User")
        }

        verify("Service will return token issuers") {
            val tokenIssuers = service.findByRoles(listOf(UserRoleType.ADMIN, UserRoleType.TOKEN_ISSUER))
            assertThat(tokenIssuers).hasSize(2)
            assertThat(tokenIssuers.map { it.uuid })
                .containsAll(listOf(testContext.user.uuid, testContext.secondUser.uuid))
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
            service.changeUserRole(testContext.user.uuid, UserRoleType.USER)
        }
        verify("User has admin role") {
            val userWithNewRole = userRepository.findById(testContext.user.uuid)
            assertThat(userWithNewRole).isPresent
            assertThat(userWithNewRole.get().role.id).isEqualTo(UserRoleType.USER.id)
        }
    }

    @Test
    fun mustBeAbleToChangeUserRoleToTokenIssuer() {
        suppose("There is user with user role") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser("admin@test.com", "Invited", "User")
            testContext.user.role = roleRepository.getOne(UserRoleType.ADMIN.id)
        }

        verify("Service can change user role to token issuer role") {
            service.changeUserRole(testContext.user.uuid, UserRoleType.TOKEN_ISSUER)
        }
        verify("User has admin role") {
            val userWithNewRole = userRepository.findById(testContext.user.uuid)
            assertThat(userWithNewRole).isPresent
            assertThat(userWithNewRole.get().role.id).isEqualTo(UserRoleType.TOKEN_ISSUER.id)
        }
    }

    @Test
    fun mustBeAbleToChangeUserRoleToPlatformManager() {
        suppose("There is user with user role") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser("user@test.com", "Invited", "User")
            testContext.user.role = roleRepository.getOne(UserRoleType.USER.id)
        }

        verify("Service can change user role to platform manager role") {
            service.changeUserRole(testContext.user.uuid, UserRoleType.PLATFORM_MANAGER)
        }
        verify("User has admin role") {
            val userWithNewRole = userRepository.findById(testContext.user.uuid)
            assertThat(userWithNewRole).isPresent
            assertThat(userWithNewRole.get().role.id).isEqualTo(UserRoleType.PLATFORM_MANAGER.id)
        }
    }

    @Test
    fun mustThrowExceptionForChangeRoleOfNonExistingUser() {
        verify("Service will throw exception") {
            val exception = assertThrows<InvalidRequestException> {
                service.changeUserRole(UUID.randomUUID(), UserRoleType.ADMIN)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.USER_MISSING)
        }
    }

    private class TestContext {
        lateinit var user: User
        lateinit var secondUser: User
        lateinit var userInfo: UserInfo
    }
}
