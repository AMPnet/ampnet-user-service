package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.Role
import com.ampnet.userservice.persistence.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): Optional<User>
    fun findByEmailContainingIgnoreCase(email: String): List<User>
    fun findByRole(role: Role): List<User>
}
