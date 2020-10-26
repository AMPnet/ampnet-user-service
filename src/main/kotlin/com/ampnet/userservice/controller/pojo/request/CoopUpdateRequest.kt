package com.ampnet.userservice.controller.pojo.request

import javax.validation.constraints.Size

data class CoopUpdateRequest(
    @field:Size(max = 128) val name: String?,
    @field:Size(max = 512) val host: String?,
    val config: Map<String, Any>?
)
