package com.ampnet.userservice.service.impl

import com.ampnet.userservice.controller.pojo.request.CreateAdminUserRequest
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.AdminService
import com.ampnet.userservice.service.pojo.UserCount
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class AdminServiceImpl(
    private val userRepository: UserRepository,
    private val userInfoRepository: UserInfoRepository,
    private val passwordEncoder: PasswordEncoder
) : AdminService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<User> {
        return userRepository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String, pageable: Pageable): Page<User> {
        return userRepository.findByEmailContainingIgnoreCase(email, pageable)
    }

    @Transactional(readOnly = true)
    override fun findByRole(role: UserRole, pageable: Pageable): Page<User> {
        return userRepository.findByRole(role, pageable)
    }

    @Transactional(readOnly = true)
    override fun findByRoles(roles: List<UserRole>): List<User> {
        return userRepository.findByRoleIn(roles.map { it })
    }

    @Transactional
    override fun createUser(request: CreateAdminUserRequest): User {
        if (userRepository.findByEmailAndCoop(request.email, request.coop).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.REG_USER_EXISTS, "Email: ${request.email} already used")
        }
        logger.info { "Creating Admin user: $request" }
        val user = User(
            UUID.randomUUID(),
            request.firstName,
            request.lastName,
            request.email,
            passwordEncoder.encode(request.password),
            AuthMethod.EMAIL,
            null,
            request.role,
            ZonedDateTime.now(),
            true,
            request.coop
        )
        return userRepository.save(user)
    }

    @Transactional
    override fun changeUserRole(userUuid: UUID, role: UserRole, coop: String): User {
        val user = userRepository.findByUuidAndCoop(userUuid, coop).orElseThrow {
            throw InvalidRequestException(ErrorCode.USER_MISSING, "Missing user with uuid: $userUuid for coop: $coop")
        }
        logger.info { "Changing user role for user: ${user.uuid} to role: $role" }
        user.role = role
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    override fun countUsers(): UserCount {
        val userInfos = userInfoRepository.findAll()
        val registeredUsers = countAllUsers()
        val activatedUsers = userInfos.filter { it.connected }.size
        val deactivatedUsers = userInfos.filter { it.deactivated }.size
        return UserCount(registeredUsers, activatedUsers, deactivatedUsers)
    }

    @Transactional(readOnly = true)
    override fun countAllUsers(): Int = userRepository.count().toInt()
}
