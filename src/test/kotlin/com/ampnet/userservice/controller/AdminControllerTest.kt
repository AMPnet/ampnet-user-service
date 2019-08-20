package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.RoleRequest
import com.ampnet.userservice.controller.pojo.response.UserResponse
import com.ampnet.userservice.controller.pojo.response.UsersListResponse
import com.ampnet.userservice.enums.PrivilegeType
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.security.WithMockCrowdfoundUser
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
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeAbleToGetAListOfUsers() {
        suppose("Some user exists in database") {
            databaseCleanerService.deleteAllUsers()
            createUser("test@email.com")
        }

        verify("The controller returns a list of users") {
            val result = mockMvc.perform(get(pathUsers))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
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
            databaseCleanerService.deleteAllUsers()
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
            databaseCleanerService.deleteAllUsers()
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
            databaseCleanerService.deleteAllUsers()
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
            databaseCleanerService.deleteAllUsers()
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
            val role = roleRepository.getOne(roleType.id)
            assertThat(userResponse.role).isEqualTo(role.name)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_PROFILE])
    fun mustBeABleToGetListOfAdminUsers() {
        suppose("There is admin and regular user") {
            databaseCleanerService.deleteAllUsers()
            testContext.user = createUser("user@role.com")
            testContext.admin = createUser("admin@role.com")
            val adminRole = roleRepository.getOne(UserRoleType.ADMIN.id)
            testContext.admin.role = adminRole
            userRepository.save(testContext.admin)
        }

        verify("Admin can get a list of only admin users") {
            val result = mockMvc.perform(get("$pathUsers/admin"))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn()

            val listResponse: UsersListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(listResponse.users).hasSize(1)
            assertThat(listResponse.users[0].uuid).isEqualTo(testContext.admin.uuid.toString())
            assertThat(listResponse.users[0].role).isEqualTo(UserRoleType.ADMIN.name)
        }
    }

    private class TestContext {
        lateinit var user: User
        lateinit var admin: User
        val email = "john@smith.com"
    }
}
