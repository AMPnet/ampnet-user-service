package com.ampnet.userservice.controller.pojo.response

import com.ampnet.userservice.service.pojo.AccessAndRefreshToken

data class AccessRefreshTokenResponse(
    val accessToken: String,
    val expiresIn: Long,
    val refreshToken: String,
    val refreshTokenExpiresIn: Long
) {
    constructor(accessAndRefreshToken: AccessAndRefreshToken) : this(
        accessAndRefreshToken.accessToken,
        accessAndRefreshToken.expiresIn,
        accessAndRefreshToken.refreshToken,
        accessAndRefreshToken.refreshTokenExpiresIn
    )
}
