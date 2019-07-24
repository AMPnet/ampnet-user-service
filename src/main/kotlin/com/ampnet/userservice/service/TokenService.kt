package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.pojo.AccessAndRefreshToken
import java.util.UUID

interface TokenService {
    fun generateAccessAndRefreshForUser(user: User): AccessAndRefreshToken
    fun generateAccessAndRefreshFromRefreshToken(token: String): AccessAndRefreshToken
    fun deleteRefreshToken(userUuid: UUID)
}
