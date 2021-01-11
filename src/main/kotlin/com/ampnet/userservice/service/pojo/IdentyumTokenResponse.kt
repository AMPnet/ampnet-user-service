package com.ampnet.userservice.service.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class IdentyumTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Long,
    @JsonProperty("refresh_expires_in")
    val refreshTokenExpiresIn: Long,
    @JsonProperty("refresh_token")
    val refreshToken: String,
    @JsonProperty("session_state")
    val sessionState: String
)
data class IdentyumTokenServiceResponse(
    val webComponentUrl: String,
    val credentials: IdentyumTokenResponse
)
