package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.UserInfo

interface VeriffService {
    fun saveUserVerificationData(data: String): UserInfo?
    fun verifyClient(client: String)
    fun verifySignature(signature: String, data: String)
}
