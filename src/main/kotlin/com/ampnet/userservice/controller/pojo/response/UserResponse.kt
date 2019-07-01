package com.ampnet.userservice.controller.pojo.response

import com.ampnet.userservice.persistence.model.User

data class UserResponse(
    val uuid: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val role: String,
    val enabled: Boolean
) {

    constructor(user: User) : this(
        user.uuid.toString(),
        user.email,
        user.userInfo.firstName,
        user.userInfo.lastName,
        user.role.name,
        user.enabled
    )
}

data class UsersListResponse(val users: List<UserResponse>)
