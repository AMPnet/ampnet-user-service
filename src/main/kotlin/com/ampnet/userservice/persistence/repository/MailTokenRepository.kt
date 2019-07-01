package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.MailToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface MailTokenRepository : JpaRepository<MailToken, Int> {
    fun findByToken(token: UUID): Optional<MailToken>
    fun findByUserUuid(userUidd: UUID): Optional<MailToken>
}
