package com.ampnet.userservice.controller.pojo.request

import com.fasterxml.jackson.annotation.JsonProperty

data class IdentyumPayloadRequest(
    @JsonProperty("webSessionUuid")
    val webSessionUuid: String,

    // typo! check with Identyum
    // @JsonProperty("prouctUuid")
    // val prouctUuid: String,

    @JsonProperty("reportUuid")
    val reportUuid: String,

    @JsonProperty("reportName")
    val reportName: String,

    val version: Int,

    @JsonProperty("outputFormat")
    val outputFormat: String,

    @JsonProperty("payloadFormat")
    val payloadFormat: String,

    @JsonProperty("processStatus")
    val processStatus: String,

    val payload: String,

    @JsonProperty("payloadSignature")
    val payloadSignature: String,

    @JsonProperty("tsCreated")
    val tsCreated: String
) {
    override fun toString(): String {
        return "(webSessionUuid=$webSessionUuid, reportUuid=$reportUuid, reportName=$reportName, version=$version, " +
            "outputFormat=$outputFormat, payloadFormat=$payloadFormat, processStatus=$processStatus, " +
            "tsCreated=$tsCreated)"
    }
}
