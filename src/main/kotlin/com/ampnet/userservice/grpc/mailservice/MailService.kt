package com.ampnet.userservice.grpc.mailservice

interface MailService {
    fun sendConfirmationMail(email: String, token: String, coop: String)
    fun sendResetPasswordMail(email: String, token: String, coop: String)
}
