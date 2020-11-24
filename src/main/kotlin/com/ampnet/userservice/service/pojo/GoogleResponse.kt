package com.ampnet.userservice.service.pojo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val mapper = jacksonObjectMapper().apply {
    propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
    configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
}
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
) {
    companion object {
        fun fromJson(json: String) = mapper.readValue<GoogleResponse>(json)
    }
}
