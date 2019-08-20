package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.enums.UserRoleType

data class CreateAdminUserRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String,
    val role: UserRoleType
)
