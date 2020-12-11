package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.User
import java.util.UUID

interface UserMailService {
    fun sendMailConfirmation(user: User)
    fun confirmEmail(token: UUID): User?
    fun resendConfirmationMail(user: User)
}
