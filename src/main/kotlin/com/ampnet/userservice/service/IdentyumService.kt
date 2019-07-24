package com.ampnet.userservice.service

import com.ampnet.userservice.controller.pojo.request.IdentyumPayloadRequest
import com.ampnet.userservice.persistence.model.UserInfo

interface IdentyumService {
    fun getToken(): String
    fun createUserInfo(request: IdentyumPayloadRequest): UserInfo
    fun findUserInfo(webSessionUuid: String): UserInfo?
}
