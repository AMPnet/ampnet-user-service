package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.UserInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface UserInfoRepository : JpaRepository<UserInfo, UUID> {
    fun findBySessionId(sessionId: String): Optional<UserInfo>

    @Query(
        "SELECT userInfo FROM UserInfo userInfo INNER JOIN User user ON userInfo.uuid = user.userInfoUuid " +
            "WHERE user.coop = :coop"
    )
    fun findAllByCoop(coop: String): List<UserInfo>
}
