package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.enums.AuthMethod

data class SignupRequest(
    val signupMethod: AuthMethod,
    val userInfo: Map<String, String>,
    val coop: String?
) {
    override fun toString(): String {
        return "SignupRequest(method: $signupMethod, coop: $coop)"
    }
}
