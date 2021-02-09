package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.enums.KycProvider
import javax.validation.constraints.Size

data class CoopUpdateRequest(
    @field:Size(max = 128) val name: String?,
    @field:Size(max = 512) val hostname: String?,
    val needUserVerification: Boolean?,
    val config: Map<String, Any>?,
    val kycProvider: KycProvider?,
    val sigUpEnabled: Boolean?
)
