package com.ampnet.userservice.service.impl

import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.AdminService
import com.ampnet.userservice.service.pojo.UserCount
import mu.KLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AdminServiceImpl(
    private val userRepository: UserRepository,
    private val userInfoRepository: UserInfoRepository
) : AdminService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun findAll(coop: String, pageable: Pageable): Page<User> = userRepository.findAllByCoop(coop, pageable)

    @Transactional(readOnly = true)
    override fun findByEmail(coop: String, email: String, pageable: Pageable): Page<User> =
        userRepository.findByCoopAndEmailContainingIgnoreCase(coop, email, pageable)

    @Transactional(readOnly = true)
    override fun findByRole(coop: String, role: UserRole, pageable: Pageable): Page<User> =
        userRepository.findByCoopAndRole(coop, role, pageable)

    @Transactional(readOnly = true)
    override fun findByRoles(coop: String, roles: List<UserRole>): List<User> =
        userRepository.findByCoopAndRoleIn(coop, roles.map { it })

    @Transactional
    override fun changeUserRole(coop: String, userUuid: UUID, role: UserRole): User {
        val user = userRepository.findByCoopAndUuid(coop, userUuid).orElseThrow {
            throw InvalidRequestException(ErrorCode.USER_MISSING, "Missing user with uuid: $userUuid for coop: $coop")
        }
        logger.info { "Changing user role for user: ${user.uuid} to role: $role" }
        user.role = role
        return userRepository.save(user)
    }

    @Transactional(readOnly = true)
    override fun countUsers(coop: String): UserCount {
        val userInfos = userInfoRepository.findAllByCoop(coop)
        val registeredUsers = userRepository.countByCoop(coop).toInt()
        val activatedUsers = userInfos.filter { it.connected }.size
        val deactivatedUsers = userInfos.filter { it.deactivated }.size
        return UserCount(registeredUsers, activatedUsers, deactivatedUsers)
    }
}
