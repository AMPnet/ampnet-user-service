package com.ampnet.userservice.amqp.mailservice

interface MailService {
    fun sendConfirmationMail(request: UserDataWithToken)
    fun sendResetPasswordMail(request: UserDataWithToken)
}
