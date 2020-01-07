package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.Role
import com.ampnet.userservice.persistence.model.User
import java.util.Optional
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): Optional<User>
    fun findByEmailContainingIgnoreCase(email: String, pageable: Pageable): Page<User>
    fun findByRole(role: Role, pageable: Pageable): Page<User>
}
