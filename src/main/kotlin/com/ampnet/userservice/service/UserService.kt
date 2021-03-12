package com.ampnet.userservice.service

import com.ampnet.userservice.controller.pojo.request.UserUpdateRequest
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.pojo.CreateUserServiceRequest
import com.ampnet.userservice.service.pojo.UserResponse
import java.util.UUID

interface UserService {
    fun createUser(request: CreateUserServiceRequest): UserResponse
    fun connectUserInfo(userUuid: UUID, sessionId: String): UserResponse
    fun find(email: String, coop: String?): User?
    fun find(userUuid: UUID): UserResponse?
    fun countAllUsers(coop: String?): Int
    fun update(userUuid: UUID, request: UserUpdateRequest): UserResponse
}
