package com.ampnet.userservice.service.pojo

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
    val coop: String,
    val language: String?
) {
    constructor(user: User, needVerification: Boolean = true) : this(
        user.uuid.toString(),
        user.email,
        user.firstName,
        user.lastName,
        user.role.name,
        user.enabled,
        if (needVerification) { (user.userInfoUuid != null || user.role != UserRole.USER) } else true,
        user.coop,
        user.language
    )
}
