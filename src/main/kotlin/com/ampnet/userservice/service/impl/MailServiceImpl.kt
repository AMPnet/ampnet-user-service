package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.service.MailService
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class MailServiceImpl(
    private val applicationProperties: ApplicationProperties
) : MailService {

    companion object : KLogging()

    override fun sendConfirmationMail(to: String, token: String) {
        if (applicationProperties.mail.enabled) {
            // TODO: implement using mail-service
        } else {
            logger.warn { "Sending email is disabled." }
        }
    }
}
