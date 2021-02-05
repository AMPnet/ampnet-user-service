package com.ampnet.userservice.service.impl

import com.ampnet.userservice.amqp.mailservice.MailService
import com.ampnet.userservice.amqp.mailservice.UserDataWithToken
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.persistence.model.MailToken
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.MailTokenRepository
import com.ampnet.userservice.service.UserMailService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class UserMailServiceImpl(
    private val mailTokenRepository: MailTokenRepository,
    private val mailService: MailService
) : UserMailService {

    @Transactional
    override fun sendMailConfirmation(user: User) {
        if (user.authMethod != AuthMethod.EMAIL) return
        val mailToken = createMailToken(user)
        mailService.sendConfirmationMail(UserDataWithToken(user, mailToken.token))
    }

    @Transactional
    @Throws(InvalidRequestException::class)
    override fun confirmEmail(token: UUID): User? {
        ServiceUtils.wrapOptional(mailTokenRepository.findByToken(token))?.let { mailToken ->
            if (mailToken.isExpired()) {
                throw InvalidRequestException(
                    ErrorCode.REG_EMAIL_EXPIRED_TOKEN,
                    "User is trying to confirm mail with expired token: $token"
                )
            }
            val user = mailToken.user
            user.enabled = true
            mailTokenRepository.delete(mailToken)
            UserServiceImpl.logger.debug { "Email confirmed for user: ${user.email}" }
            return user
        }
        return null
    }

    @Transactional
    override fun resendConfirmationMail(user: User) {
        if (user.authMethod != AuthMethod.EMAIL) return
        ServiceUtils.wrapOptional(mailTokenRepository.findByUserUuid(user.uuid))?.let {
            mailTokenRepository.delete(it)
        }
        val mailToken = createMailToken(user)
        mailService.sendConfirmationMail(UserDataWithToken(user, mailToken.token))
    }

    private fun createMailToken(user: User): MailToken {
        val mailToken = MailToken(0, user, UUID.randomUUID(), ZonedDateTime.now())
        return mailTokenRepository.save(mailToken)
    }
}
