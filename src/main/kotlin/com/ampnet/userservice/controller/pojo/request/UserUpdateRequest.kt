package com.ampnet.userservice.controller.pojo.request

import javax.validation.constraints.Size

data class UserUpdateRequest(
    @field:Size(max = 8) val language: String
)
