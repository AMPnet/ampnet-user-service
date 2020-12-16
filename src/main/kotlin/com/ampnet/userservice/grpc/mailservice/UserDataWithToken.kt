package com.ampnet.userservice.grpc.mailservice

import com.ampnet.userservice.persistence.model.User
import java.util.UUID

data class UserDataWithToken(
    val email: String,
    val coop: String,
    val language: String,
    val token: String
) {
    constructor(user: User, token: UUID) : this(
        user.email, user.coop, user.language ?: "", token.toString()
    )

    constructor(user: User, token: String) : this(
        user.email, user.coop, user.language ?: "", token
    )
}
