package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.validation.EmailConstraint
import javax.validation.constraints.Size

data class CreateAdminUserRequest(
    @field:EmailConstraint
    val email: String,
    @field:Size(max = 256)
    val firstName: String,
    @field:Size(max = 256)
    val lastName: String,
    val password: String,
    val role: UserRole,
    val coop: String
) {
    override fun toString(): String {
        return "CreateAdminUserRequest(email: $email, firstName: $firstName, lastName: $lastName, role: $role, " +
            "coop: $coop)"
    }
}
