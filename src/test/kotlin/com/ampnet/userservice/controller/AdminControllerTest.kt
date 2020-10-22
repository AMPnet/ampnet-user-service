package com.ampnet.userservice.controller

import com.ampnet.userservice.COOP
import com.ampnet.userservice.controller.pojo.request.RoleRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.controller.pojo.response.UsersListResponse
import com.ampnet.userservice.enums.PrivilegeType
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.security.WithMockCrowdfundUser
import com.ampnet.userservice.service.pojo.UserCount
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminControllerTest : ControllerTestBase() {

    private val pathUsers = "/admin/user"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun initTestData() {
        databaseCleanerService.deleteAllUsers()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetAListOfUsers() {
        suppose("Some user exists in database") {
            createUser("test@email.com")
        }
        suppose("There is one user in another coop") {
            createUser("another@coop.com", coop = "another-coop")
        }

        verify("The controller returns a list of users") {
            val result = mockMvc.perform(get(pathUsers))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(1)
        }
    }

    @Test
    @WithMockCrowdfundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetPageableListOfUsers() {
        suppose("Some users exist in database") {
            createUser("test@email.com")
            createUser("test2@email.com")
            createUser("test22@email.com")
            createUser("test23@email.com")
            createUser("test24@email.com")
        }
        suppose("There is one user in another coop") {
            createUser("another@coop.com", coop = "another-coop")
        }

        verify("The controller returns pageable list of users") {
            val result = mockMvc.perform(
                get(pathUsers)
                    .param("size", "3")
                    .param("page", "1")
                    .param("sort", "email,asc")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(2)
            assertThat(listResponse.page).isEqualTo(1)
            assertThat(listResponse.totalPages).isEqualTo(2)
        }
    }

    @Test
    @WithMockCrowdfundUser(role = UserRole.USER)
    fun mustNotBeAbleToGetAListOfUsersWithoutAdminPermission() {
        verify("The user with role USER cannot fetch a list of users") {
            mockMvc.perform(get(pathUsers))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToFindUsersByEmail() {
        suppose("User exists") {
            testContext.user = createUser(testContext.email)
            createUser("john.wayne@mail.com")
        }
        suppose("There is one user in another coop") {
            createUser("john.wayne@mail.com", coop = "another-coop")
        }

        verify("Admin can find user by email") {
            val result = mockMvc.perform(
                get("$pathUsers/find")
                    .param("email", "john")
                    .param("size", "20")
                    .param("page", "0")
            )
                .andExpect(status().isOk)
                .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(2)
            assertThat(listResponse.page).isEqualTo(0)
            assertThat(listResponse.totalPages).isEqualTo(1)
        }
    }

    @Test
    @WithMockCrowdfundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun adminMustBeAbleToGetUserByUuid() {
        suppose("User exists in database") {
            testContext.user = createUser(testContext.email)
        }

        verify("Admin can get user by uuid") {
            val result = mockMvc.perform(get("$pathUsers/${testContext.user.uuid}"))
                .andExpect(status().isOk)
                .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testContext.user.email)
        }
    }

    @Test
    @WithMockCrowdfundUser(role = UserRole.USER)
    fun mustNotBeAbleToChangeRoleWithUserRole() {
        suppose("User is in database") {
            testContext.user = createUser(testContext.email)
        }

        verify("Controller will return forbidden because privilege is missing") {
            val request = RoleRequest(UserRole.ADMIN)
            mockMvc.perform(
                post("$pathUsers/${testContext.user.uuid}/role")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfundUser(privileges = [PrivilegeType.PWA_PROFILE], coop = COOP)
    fun mustBeAbleToChangeRoleWithPrivilege() {
        suppose("User with admin role is in database") {
            testContext.user = createUser("admin@role.com", role = UserRole.ADMIN)
        }

        verify("Admin user can change user role") {
            val roleType = UserRole.USER
            val request = RoleRequest(roleType)
            val result = mockMvc.perform(
                post("$pathUsers/${testContext.user.uuid}/role")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testContext.user.email)
            assertThat(userResponse.role).isEqualTo(roleType.name)
        }
        verify("User role has admin role") {
            val optionalUser = userRepository.findById(testContext.user.uuid)
            assertThat(optionalUser).isPresent
            assertThat(optionalUser.get().role).isEqualTo(UserRole.USER)
        }
    }

    @Test
    @WithMockCrowdfundUser(privileges = [PrivilegeType.PWA_PROFILE])
    fun mustNotBeAbleToChangeRoleToAdmin() {
        suppose("User with admin role is in database") {
            testContext.user = createUser("user@role.com", role = UserRole.USER)
        }

        verify("Admin user can change user role") {
            val roleType = UserRole.ADMIN
            val request = RoleRequest(roleType)
            val result = mockMvc.perform(
                post("$pathUsers/${testContext.user.uuid}/role")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.USER_ROLE_INVALID)
        }
    }

    @Test
    @WithMockCrowdfundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeABleToGetListOfAdminUsers() {
        suppose("There is admin and regular user") {
            testContext.user = createUser("user@role.com")
            testContext.admin = createAdminUser()
        }
        suppose("There is admin user in another coop") {
            createAdminUser("another-coop")
        }

        verify("Admin can get a list of only admin users") {
            val result = mockMvc.perform(
                get("$pathUsers/admin")
                    .param("size", "10")
                    .param("page", "0")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(1)
            assertThat(listResponse.users[0].uuid).isEqualTo(testContext.admin.uuid.toString())
            assertThat(listResponse.users[0].role).isEqualTo(UserRole.ADMIN.name)
            assertThat(listResponse.page).isEqualTo(0)
            assertThat(listResponse.totalPages).isEqualTo(1)
        }
    }

    @Test
    @WithMockCrowdfundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetUserCount() {
        suppose("There is admin user") {
            databaseCleanerService.deleteAllUserInfos()
            testContext.admin = createAdminUser()
        }
        suppose("There is registered user info") {
            createUser("registered@email.com")
        }
        suppose("There is connected user info") {
            createUserWithUserInfo("connected@user.com")
        }
        suppose("There is disabled user") {
            createUserWithUserInfo("disabled@user.com", disabled = true)
        }
        suppose("There are users in another cops") {
            createUser("another@coop.com", coop = "another")
            createUserWithUserInfo("connected@user.com", coop = "an")
            createUserWithUserInfo("disabled@user.com", disabled = true, coop = "oooo")
        }

        verify("Admin can get user count") {
            val result = mockMvc.perform(get("$pathUsers/count"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val countResponse: UserCount = objectMapper.readValue(result.response.contentAsString)
            assertThat(countResponse.registered).isEqualTo(4)
            assertThat(countResponse.activated).isEqualTo(2)
            assertThat(countResponse.deleted).isEqualTo(1)
        }
    }

    private fun createAdminUser(coop: String = COOP): User {
        val admin = createUser("admin@role.com", coop = coop)
        val adminRole = UserRole.ADMIN
        admin.role = adminRole
        userRepository.save(admin)
        return admin
    }

    private fun createUserWithUserInfo(email: String, disabled: Boolean = false, coop: String = COOP): User {
        val user = createUser(email, coop = coop)
        val userInfo = createUserInfo(email = email, disabled = disabled)
        user.userInfoId = userInfo.id
        return userRepository.save(user)
    }

    private class TestContext {
        lateinit var user: User
        lateinit var admin: User
        val email = "john@smith.com"
    }
}
