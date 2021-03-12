package com.ampnet.userservice.service

import com.ampnet.userservice.service.pojo.UserResponse
import java.util.UUID

interface PasswordService {
    fun changePassword(userUuid: UUID, oldPassword: String, newPassword: String): UserResponse
    fun changePasswordWithToken(token: UUID, newPassword: String): UserResponse
    fun generateForgotPasswordToken(email: String, coop: String?): Boolean
    fun verifyPasswords(password: String, encodedPassword: String?): Boolean
}
