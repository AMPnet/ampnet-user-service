package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import java.util.UUID

interface UserService {
    fun createUser(request: CreateUserServiceRequest): User
    fun connectUserInfo(userUuid: UUID, webSessionUuid: String): User
    fun find(email: String): User?
    fun find(userUuid: UUID): User?
    fun confirmEmail(token: UUID): User?
    fun resendConfirmationMail(user: User)
}
