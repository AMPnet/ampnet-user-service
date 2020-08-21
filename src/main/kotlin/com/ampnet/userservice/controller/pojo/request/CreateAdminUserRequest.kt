package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.enums.UserRoleType
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
    val role: UserRoleType
) {
    override fun toString(): String {
        return "CreateAdminUserRequest(email: $email, firstName: $firstName, lastName: $lastName, role: $role)"
    }
}
