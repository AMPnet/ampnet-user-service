package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.validation.EmailConstraint

data class TokenRequestUserInfo(
    @EmailConstraint
    val email: String,
    val password: String
)
