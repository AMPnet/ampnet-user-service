package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.model.VeriffSession
import com.ampnet.userservice.service.pojo.ServiceVerificationResponse
import java.util.UUID

interface VeriffService {
    fun getVeriffSession(userUuid: UUID, baseUrl: String): ServiceVerificationResponse?
    fun handleDecision(data: String): UserInfo?
    fun handleEvent(data: String): VeriffSession?
    fun verifyClient(client: String)
    fun verifySignature(signature: String, data: String)
}
