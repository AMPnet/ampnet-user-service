package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.CreateAdminUserRequest
import com.ampnet.userservice.controller.pojo.request.RoleRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.controller.pojo.response.UsersListResponse
import com.ampnet.userservice.enums.PrivilegeType
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.security.WithMockCrowdfoundUser
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
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_PROFILE])
    fun mustBeAbleToCreateAdminUser() {
        verify("Admin can create admin user") {
            val roleType = UserRoleType.ADMIN
            val request = CreateAdminUserRequest(testContext.email, "first", "last", "password", roleType)
            val result = mockMvc.perform(
                post(pathUsers)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testContext.email)
            assertThat(userResponse.role).isEqualTo(roleType.name)
        }
        verify("Admin user is created") {
            val optionalUser = userRepository.findByEmail(testContext.email)
            assertThat(optionalUser).isPresent
            assertThat(optionalUser.get().role.name).isEqualTo(UserRoleType.ADMIN.name)
            assertThat(optionalUser.get().userInfo).isNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetAListOfUsers() {
        suppose("Some user exists in database") {
            createUser("test@email.com")
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
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustNotBeAbleToGetAListOfUsersWithoutAdminPermission() {
        verify("The user with role USER cannot fetch a list of users") {
            mockMvc.perform(get(pathUsers))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToFindUsersByEmail() {
        suppose("User exists") {
            testContext.user = createUser(testContext.email)
            createUser("john.wayne@mail.com")
        }

        verify("Admin can find user by email") {
            val result = mockMvc.perform(get("$pathUsers/find").param("email", "john"))
                .andExpect(status().isOk)
                .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(2)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
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
    @WithMockCrowdfoundUser(role = UserRoleType.USER)
    fun mustNotBeAbleToChangeRoleWithUserRole() {
        suppose("User is in database") {
            testContext.user = createUser(testContext.email)
        }

        verify("Controller will return forbidden because privilege is missing") {
            val request = RoleRequest(UserRoleType.ADMIN)
            mockMvc.perform(
                post("$pathUsers/${testContext.user.uuid}/role")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_PROFILE])
    fun mustBeAbleToChangeRoleWithPrivilege() {
        suppose("User with user role is in database") {
            testContext.user = createUser("user@role.com")
        }

        verify("Admin user can change user role") {
            val roleType = UserRoleType.ADMIN
            val request = RoleRequest(roleType)
            val result = mockMvc.perform(
                post("$pathUsers/${testContext.user.uuid}/role")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()

            val userResponse: UserResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userResponse.email).isEqualTo(testContext.user.email)
            assertThat(userResponse.role).isEqualTo(roleType.name)
        }
        verify("User role has admin role") {
            val optionalUser = userRepository.findById(testContext.user.uuid)
            assertThat(optionalUser).isPresent
            assertThat(optionalUser.get().role.name).isEqualTo(UserRoleType.ADMIN.name)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeABleToGetListOfAdminUsers() {
        suppose("There is admin and regular user") {
            testContext.user = createUser("user@role.com")
            testContext.admin = createAdminUser()
        }

        verify("Admin can get a list of only admin users") {
            val result = mockMvc.perform(get("$pathUsers/admin"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(1)
            assertThat(listResponse.users[0].uuid).isEqualTo(testContext.admin.uuid.toString())
            assertThat(listResponse.users[0].role).isEqualTo(UserRoleType.ADMIN.name)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetUserCount() {
        suppose("There is admin user") {
            testContext.admin = createAdminUser()
        }
        suppose("There is registered user info") {
            createUserInfo(connected = false)
        }
        suppose("There is connected user info") {
            createUserInfo(connected = true)
        }
        suppose("There is disabled user") {
            createUserInfo(connected = true, disabled = true)
        }

        verify("Admin can get user count") {
            val result = mockMvc.perform(get("$pathUsers/count"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val countResponse: UserCount = objectMapper.readValue(result.response.contentAsString)
            assertThat(countResponse.registered).isEqualTo(3)
            assertThat(countResponse.activated).isEqualTo(2)
            assertThat(countResponse.deleted).isEqualTo(1)
        }
    }

    private fun createAdminUser(): User {
        val admin = createUser("admin@role.com")
        val adminRole = roleRepository.getOne(UserRoleType.ADMIN.id)
        admin.role = adminRole
        userRepository.save(admin)
        return admin
    }

    private class TestContext {
        lateinit var user: User
        lateinit var admin: User
        val email = "john@smith.com"
    }
}
