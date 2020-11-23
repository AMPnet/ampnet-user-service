package com.ampnet.userservice.service.pojo

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.HashMap

data class GoogleResponse(
    @JsonProperty("challenge_ts")
    val challengeTs: String,
    @JsonProperty("error-codes")
    val errorCodes: List<String>,
    @JsonProperty("hostname")
    val hostname: String,
    @JsonProperty("success")
    val success: Boolean
)

data class ReCaptchaGoogleResponse(
    @JsonProperty("success")
    val success: Boolean,

    @JsonProperty("challenge_ts")
    val challengeTs: String,

    @JsonProperty("hostname")
    private val hostname: String,

    @JsonProperty("error-codes")
    val errorCodes: List<ErrorCode>
) {

    enum class ErrorCode {
        MissingSecret, InvalidSecret, MissingResponse,
        InvalidResponse, BadRequest, TimeoutOrDuplicate;

        companion object {
            private val errorsMap: MutableMap<String, ErrorCode> = HashMap(4)

            @JsonCreator
            fun fromValue(value: String) = errorsMap[value.toLowerCase()]

            init {
                errorsMap["missing-input-secret"] = MissingSecret
                errorsMap["invalid-input-secret"] = InvalidSecret
                errorsMap["missing-input-response"] = MissingResponse
                errorsMap["invalid-input-response"] = InvalidResponse
                errorsMap["bad-request"] = BadRequest
                errorsMap["timeout-or-duplicate"] = TimeoutOrDuplicate
            }
        }
    }
}
