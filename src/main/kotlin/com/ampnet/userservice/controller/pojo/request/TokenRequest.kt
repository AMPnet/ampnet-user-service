package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.enums.AuthMethod

data class TokenRequest(
    val loginMethod: AuthMethod,
    val credentials: Map<String, String>,
    val coop: String?
) {
    override fun toString(): String {
        return "TokenRequest(loginMethod: $loginMethod, coop: $coop)"
    }
}
