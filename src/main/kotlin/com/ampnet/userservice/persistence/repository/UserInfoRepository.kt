package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.UserInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface UserInfoRepository : JpaRepository<UserInfo, Int> {
    fun findBySessionId(sessionId: String): Optional<UserInfo>

    @Query(
        "SELECT userInfo FROM UserInfo userInfo INNER JOIN User user ON userInfo.id = user.userInfoId " +
            "WHERE user.coop = :coop"
    )
    fun findAllByCoop(coop: String): List<UserInfo>
}
