package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.validation.EmailConstraint
import javax.validation.constraints.Size

data class SignupRequestUserInfo(
    @field:Size(max = 256)
    val firstName: String?,
    @field:Size(max = 256)
    val lastName: String?,
    @field:EmailConstraint
    val email: String,
    val password: String
) {
    override fun toString(): String = "SignupRequestUserInfo(email: $email, firstName: $firstName, lastName: $lastName)"
}
