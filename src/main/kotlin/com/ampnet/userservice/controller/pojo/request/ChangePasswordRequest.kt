package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.validation.PasswordConstraint
import java.util.UUID
import javax.validation.constraints.NotEmpty

data class ChangePasswordRequest(
    @field:NotEmpty val oldPassword: String,
    @field:PasswordConstraint val newPassword: String
) {
    override fun toString(): String {
        return "ChangePasswordRequest(cannot show password)"
    }
}

data class ChangePasswordTokenRequest(@field:PasswordConstraint val newPassword: String, val token: UUID) {
    override fun toString(): String {
        return "ChangePasswordTokenRequest(token: $token)"
    }
}
