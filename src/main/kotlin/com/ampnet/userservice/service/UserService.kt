package com.ampnet.userservice.service

import com.ampnet.userservice.controller.pojo.request.UserUpdateRequest
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import java.util.UUID

interface UserService {
    fun createUser(request: CreateUserServiceRequest): User
    fun connectUserInfo(userUuid: UUID, sessionId: String): User
    fun find(email: String, coop: String?): User?
    fun find(userUuid: UUID): User?
    fun countAllUsers(coop: String?): Int
    fun update(userUuid: UUID, request: UserUpdateRequest): User
}
