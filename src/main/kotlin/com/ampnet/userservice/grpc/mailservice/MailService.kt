package com.ampnet.userservice.grpc.mailservice

interface MailService {
    fun sendConfirmationMail(request: UserDataWithToken)
    fun sendResetPasswordMail(request: UserDataWithToken)
}
