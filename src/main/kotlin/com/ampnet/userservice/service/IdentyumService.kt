package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.service.pojo.IdentyumTokenServiceResponse
import java.util.UUID

interface IdentyumService {
    fun getToken(user: UUID): IdentyumTokenServiceResponse
    fun createUserInfo(report: String, secretKey: String, signature: String): UserInfo?
}
