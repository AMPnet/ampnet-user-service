package com.ampnet.userservice.service

interface MailService {
    fun sendConfirmationMail(email: String, token: String)
}
