package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.UserInfo
import java.util.UUID

interface IdentyumService {
    fun getToken(user: UUID): String
    fun createUserInfo(report: String, secretKey: String, signature: String): UserInfo
}
