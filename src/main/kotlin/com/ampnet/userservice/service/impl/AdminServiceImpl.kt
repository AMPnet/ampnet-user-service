package com.ampnet.userservice.service.impl

import com.ampnet.userservice.controller.pojo.request.CreateAdminUserRequest
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.persistence.model.Role
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.RoleRepository
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.AdminService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class AdminServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userInfoRepository: UserInfoRepository,
    private val passwordEncoder: PasswordEncoder
) : AdminService {

    private val userRole: Role by lazy { roleRepository.getOne(UserRoleType.USER.id) }
    private val adminRole: Role by lazy { roleRepository.getOne(UserRoleType.ADMIN.id) }

    @Transactional(readOnly = true)
    override fun findAll(): List<User> {
        return userRepository.findAll()
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): List<User> {
        return userRepository.findByEmailContainingIgnoreCase(email)
    }

    @Transactional(readOnly = true)
    override fun findByRole(role: UserRoleType): List<User> {
        return userRepository.findByRole(getRole(role))
    }

    @Transactional
    override fun createAdminUser(request: CreateAdminUserRequest): User {
        if (userRepository.findByEmail(request.email).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.REG_USER_EXISTS, "Email: ${request.email} already used")
        }
        val userInfo = createAdminUserInfo(request)
        val user = User(
            UUID.randomUUID(),
            request.email,
            passwordEncoder.encode(request.password),
            AuthMethod.EMAIL,
            userInfo,
            getRole(request.role),
            ZonedDateTime.now(),
            true
        )
        return userRepository.save(user)
    }

    @Transactional
    override fun changeUserRole(userUuid: UUID, role: UserRoleType): User {
        val user = userRepository.findById(userUuid).orElseThrow {
            throw InvalidRequestException(ErrorCode.USER_MISSING, "Missing user with id: $userUuid")
        }

        user.role = getRole(role)
        return userRepository.save(user)
    }

    private fun getRole(role: UserRoleType) = when (role) {
        UserRoleType.ADMIN -> adminRole
        UserRoleType.USER -> userRole
    }

    private fun createAdminUserInfo(request: CreateAdminUserRequest): UserInfo {
        val userInfo = UserInfo(0,
            UUID.randomUUID().toString(),
            request.firstName,
            request.lastName,
            request.email,
            "+0",
            "NON",
            "00-00-0000",
            "0000-0000-0000-0000",
            "NON",
            "000000",
            "NON",
            false,
            "admin",
            "admin",
            "admin",
            ZonedDateTime.now(),
            true
        )
        return userInfoRepository.save(userInfo)
    }
}
