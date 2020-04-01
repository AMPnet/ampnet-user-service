package com.ampnet.userservice.service

import com.ampnet.userservice.config.JsonConfig
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.impl.AdminServiceImpl
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.annotation.Import

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
    }
}
