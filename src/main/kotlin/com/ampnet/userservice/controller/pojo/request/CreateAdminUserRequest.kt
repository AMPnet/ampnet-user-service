package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.validation.EmailConstraint

data class CreateAdminUserRequest(
    @EmailConstraint
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String,
    val role: UserRoleType
)
