package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.persistence.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findAllByCoop(coop: String, pageable: Pageable): Page<User>
    fun findByCoopAndUuid(coop: String, uuid: UUID): Optional<User>
    fun findByCoopAndEmail(coop: String, email: String): Optional<User>
    fun findByCoopAndEmailContainingIgnoreCase(coop: String, email: String, pageable: Pageable): Page<User>
    fun findByCoopAndRole(coop: String, role: UserRole, pageable: Pageable): Page<User>
    fun findByCoopAndRoleIn(coop: String, roles: List<UserRole>): List<User>
    fun countByCoop(coop: String): Long
    fun findByCoopAndEmailIn(coop: String, emails: List<String>): List<User>
    fun findAllByCoopAndUserInfoUuidIsNotNull(coop: String): List<User>
}
