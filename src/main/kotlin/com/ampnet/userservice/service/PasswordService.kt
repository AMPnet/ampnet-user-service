package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.User
import java.util.UUID

interface PasswordService {
    fun changePassword(user: User, oldPassword: String, newPassword: String): User
    fun changePasswordWithToken(token: UUID, newPassword: String): User
    fun generateForgotPasswordToken(email: String, coop: String): Boolean
    fun verifyPasswords(password: String, encodedPassword: String?): Boolean
}
