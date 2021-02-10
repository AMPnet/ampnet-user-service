package com.ampnet.userservice.amqp.mailservice

import mu.KLogging
import org.springframework.amqp.AmqpException
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

@Service
class MailServiceQueueSender(private val rabbitTemplate: RabbitTemplate) : MailService {

    companion object : KLogging()

    @Bean
    fun mailConfirmationQueue(): Queue = Queue(QUEUE_USER_MAIL_CONFIRMATION)

    @Bean
    fun mailResetPasswordQueue(): Queue = Queue(QUEUE_USER_RESET_PASSWORD)

    override fun sendConfirmationMail(request: UserDataWithToken) {
        val message = MailConfirmationMessage(request.email, request.token, request.coop, request.token)
        sendMessage(QUEUE_USER_MAIL_CONFIRMATION, message)
    }

    override fun sendResetPasswordMail(request: UserDataWithToken) {
        val message = MailResetPasswordMessage(request.email, request.token, request.coop, request.token)
        sendMessage(QUEUE_USER_RESET_PASSWORD, message)
    }

    private fun sendMessage(queue: String, message: Any) {
        try {
            logger.debug { "Sending to queue: $queue, message: $message" }
            rabbitTemplate.convertAndSend(queue, message)
        } catch (ex: AmqpException) {
            logger.warn(ex) { "Failed to send AMQP message to queue: $queue" }
        }
    }
}

const val QUEUE_USER_MAIL_CONFIRMATION = "mail.user.confirmation"
const val QUEUE_USER_RESET_PASSWORD = "mail.user.reset-password"
