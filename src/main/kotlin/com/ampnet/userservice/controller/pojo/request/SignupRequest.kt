package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.enums.AuthMethod

data class SignupRequest(
    val coop: String,
    val signupMethod: AuthMethod,
    val userInfo: Map<String, String>
) {
    override fun toString(): String {
        return "SignupRequest(method: $signupMethod, coop: $coop)"
    }
}
