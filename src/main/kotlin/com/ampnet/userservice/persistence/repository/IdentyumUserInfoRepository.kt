package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.IdentyumUserInfo
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentyumUserInfoRepository : JpaRepository<IdentyumUserInfo, UUID> {
    fun findByClientSessionUuid(clientSessionUuid: String): IdentyumUserInfo?
}
