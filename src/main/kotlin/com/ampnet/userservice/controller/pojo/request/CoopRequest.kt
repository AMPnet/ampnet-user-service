package com.ampnet.userservice.controller.pojo.request

import javax.validation.constraints.Size

data class CoopRequest(
    @field:Size(max = 128) val name: String
)
