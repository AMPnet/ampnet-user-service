package com.ampnet.userservice.service.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class GoogleResponse(

    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("score")
    val score: Float,

    @JsonProperty("challenge_ts")
    val challengeTs: String?,

    @JsonProperty("hostname")
    val hostname: String?,

    @JsonProperty("error-codes")
    val errorCodes: List<String> = emptyList()
)
