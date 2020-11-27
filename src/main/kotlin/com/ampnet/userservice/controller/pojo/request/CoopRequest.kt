package com.ampnet.userservice.controller.pojo.request

import javax.validation.constraints.Size

data class CoopRequest(
    @field:Size(max = 64) val identifier: String,
    @field:Size(max = 128) val name: String,
    @field:Size(max = 512) val hostname: String?,
    val config: Map<String, Any>?,
    val reCaptchaToken: String?
)
