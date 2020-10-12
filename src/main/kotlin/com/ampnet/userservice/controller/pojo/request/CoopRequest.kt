package com.ampnet.userservice.controller.pojo.request

import java.net.URL
import javax.validation.constraints.Size

data class CoopRequest(
    @field:Size(max = 128) val name: String,
    val url: URL
)
