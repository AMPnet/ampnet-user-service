package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.validation.EmailConstraint

data class TokenRequestUserInfo(
    @field:EmailConstraint
    val email: String,
    val password: String
) {
    override fun toString(): String {
        return "TokenRequestUserInfo(email: $email)"
    }
}
