package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.service.pojo.ServiceVerificationResponse
import java.util.UUID

interface VeriffService {
    fun getVeriffSession(userUuid: UUID): ServiceVerificationResponse?
    fun saveUserVerificationData(data: String): UserInfo?
    fun verifyClient(client: String)
    fun verifySignature(signature: String, data: String)
}
