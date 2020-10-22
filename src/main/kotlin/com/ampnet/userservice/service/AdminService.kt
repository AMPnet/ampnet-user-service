package com.ampnet.userservice.service

import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.pojo.UserCount
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface AdminService {
    fun findAll(coop: String?, pageable: Pageable): Page<User>
    fun findByEmail(coop: String, email: String, pageable: Pageable): Page<User>
    fun findByRole(coop: String, role: UserRole, pageable: Pageable): Page<User>
    fun findByRoles(coop: String, roles: List<UserRole>): List<User>
    fun changeUserRole(coop: String, userUuid: UUID, role: UserRole): User
    fun countUsers(coop: String?): UserCount
    fun countAllUsers(coop: String?): Int
}
