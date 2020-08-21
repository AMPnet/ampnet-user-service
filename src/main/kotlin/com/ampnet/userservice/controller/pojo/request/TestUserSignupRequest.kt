package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.validation.EmailConstraint

data class TestUserSignupRequest(@field:EmailConstraint val email: String, val password: String) {
    override fun toString(): String {
        return "TestUserSignupRequest(email: $email)"
    }
}
