package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.VeriffSession
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VeriffSessionRepository : JpaRepository<VeriffSession, String> {
    fun findByUserUuidOrderByCreatedAtDesc(userUuid: UUID): List<VeriffSession>
}
