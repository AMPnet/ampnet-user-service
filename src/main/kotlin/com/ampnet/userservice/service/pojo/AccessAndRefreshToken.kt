package com.ampnet.userservice.service.pojo

data class AccessAndRefreshToken(
    val accessToken: String,
    val expiresIn: Long,
    val refreshToken: String,
    val refreshTokenExpiresIn: Long
)
