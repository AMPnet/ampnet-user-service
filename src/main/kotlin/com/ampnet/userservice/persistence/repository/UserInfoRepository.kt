package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.UserInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserInfoRepository : JpaRepository<UserInfo, UUID> {
    fun findBySessionIdOrderByCreatedAtDesc(sessionId: String): List<UserInfo>

    @Query(
        "SELECT userInfo FROM UserInfo userInfo INNER JOIN User user ON userInfo.uuid = user.userInfoUuid " +
            "WHERE user.coop = :coop AND userInfo.connected = true"
    )
    fun findAllConnectedByCoop(coop: String): List<UserInfo>
}
