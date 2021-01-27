package com.ampnet.userservice.config

import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import io.sentry.EventProcessor
import io.sentry.SentryEvent
import org.springframework.stereotype.Component

class SentryConfig

@Component
class CustomBeforeSendCallback : EventProcessor {
    override fun process(event: SentryEvent, hint: Any?): SentryEvent? {
        return when (event.throwable) {
            is InvalidRequestException -> null
            is ResourceAlreadyExistsException -> null
            else -> event
        }
    }
}
