package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.Coop
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CoopRepository : JpaRepository<Coop, Int> {
    fun findByIdentifier(identifier: String): Optional<Coop>
}
