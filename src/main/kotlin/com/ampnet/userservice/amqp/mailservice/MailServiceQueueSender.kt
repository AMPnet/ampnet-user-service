package com.ampnet.userservice.amqp.mailservice

import mu.KLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class MailServiceQueueSender(private val rabbitTemplate: RabbitTemplate) : MailService {

    companion object : KLogging()

    override fun sendConfirmationMail(request: UserDataWithToken) {
        val message = MailConfirmationMessage(request.email, request.token, request.coop, request.token)
        logger.debug { "Sending mail confirmation: $message" }
        rabbitTemplate.convertAndSend(QUEUE_USER_MAIL_CONFIRMATION, message)
    }

    override fun sendResetPasswordMail(request: UserDataWithToken) {
        val message = MailResetPasswordMessage(request.email, request.token, request.coop, request.token)
        logger.debug { "Sending reset password: $message" }
        rabbitTemplate.convertAndSend(QUEUE_USER_RESET_PASSWORD, message)
    }
}

const val QUEUE_USER_MAIL_CONFIRMATION = "mail.user.confirmation"
const val QUEUE_USER_RESET_PASSWORD = "mail.user.reset-password"
