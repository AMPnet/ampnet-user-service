package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.IdentyumUserInfo

interface IdentyumService {
    fun getToken(): String
    fun createUserInfo(report: String, secretKey: String, signature: String): IdentyumUserInfo
}
