package com.ampnet.userservice.controller.pojo.response

import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.persistence.model.User

data class UserResponse(
    val uuid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val enabled: Boolean,
    val verified: Boolean,
    val coop: String
) {
    constructor(user: User) : this(
        user.uuid.toString(),
        user.email,
        user.firstName,
        user.lastName,
        user.role.name,
        user.enabled,
        user.userInfoUuid != null || user.role == UserRole.ADMIN,
        user.coop
    )
}

data class UsersListResponse(val users: List<UserResponse>, val page: Int, val totalPages: Int)
