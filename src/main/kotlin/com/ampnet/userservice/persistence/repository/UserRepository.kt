package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.persistence.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): Optional<User>
    fun findByEmailContainingIgnoreCase(email: String, pageable: Pageable): Page<User>
    fun findByRole(role: UserRole, pageable: Pageable): Page<User>
    fun findByRoleIn(roles: List<UserRole>): List<User>
}
