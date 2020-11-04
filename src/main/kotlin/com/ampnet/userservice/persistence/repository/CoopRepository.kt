package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.Coop
import org.springframework.data.jpa.repository.JpaRepository

interface CoopRepository : JpaRepository<Coop, String> {
    fun findByIdentifier(identifier: String): Coop?
    fun findByHostname(hostname: String): Coop?
}
