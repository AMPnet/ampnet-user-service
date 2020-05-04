package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.UserInfo
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

interface UserInfoRepository : JpaRepository<UserInfo, Int> {
    fun findByUserSessionUuid(userSessionUuid: String): Optional<UserInfo>
}
