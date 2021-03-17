package com.ampnet.userservice.amqp.mailservice

data class MailConfirmationMessage(
    val email: String,
    val token: String,
    val coop: String,
    val language: String
)

data class MailResetPasswordMessage(
    val email: String,
    val token: String,
    val coop: String,
    val language: String
)
