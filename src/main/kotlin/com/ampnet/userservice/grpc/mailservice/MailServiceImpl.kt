package com.ampnet.userservice.grpc.mailservice

import com.ampnet.mailservice.proto.Empty
import com.ampnet.mailservice.proto.MailConfirmationRequest
import com.ampnet.mailservice.proto.MailServiceGrpc
import com.ampnet.mailservice.proto.ResetPasswordRequest
import com.ampnet.userservice.config.ApplicationProperties
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class MailServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : MailService {

    companion object : KLogging()

    private val mailServiceStub: MailServiceGrpc.MailServiceStub by lazy {
        val channel = grpcChannelFactory.createChannel("mail-service")
        MailServiceGrpc.newStub(channel)
    }

    override fun sendConfirmationMail(request: UserDataWithToken) {
        logger.debug { "Sending confirmation mail to: ${request.email}" }
        try {
            val mailRequest = MailConfirmationRequest.newBuilder()
                .setEmail(request.email)
                .setToken(request.token)
                .setCoop(request.coop)
                .setLanguage(request.language)
                .build()
            serviceWithTimeout()
                .sendMailConfirmation(mailRequest, getStreamObserver("confirmation mail to: ${request.email}"))
        } catch (ex: StatusRuntimeException) {
            logger.warn("Failed to send confirmation mail. ${ex.localizedMessage}")
        }
    }

    override fun sendResetPasswordMail(request: UserDataWithToken) {
        logger.debug { "Sending reset password mail to: ${request.email}" }
        try {
            val mailRequest = ResetPasswordRequest.newBuilder()
                .setEmail(request.email)
                .setToken(request.token)
                .setCoop(request.coop)
                .setLanguage(request.language)
                .build()
            serviceWithTimeout()
                .sendResetPassword(mailRequest, getStreamObserver("reset password mail to: ${request.email}"))
        } catch (ex: StatusRuntimeException) {
            logger.warn { "Failed to send reset password mail. ${ex.localizedMessage}" }
        }
    }

    private fun serviceWithTimeout() = mailServiceStub
        .withDeadlineAfter(applicationProperties.grpc.mailServiceTimeout, TimeUnit.MILLISECONDS)

    private fun getStreamObserver(message: String) = object : StreamObserver<Empty> {
        override fun onNext(value: Empty?) {
            logger.info { "Successfully sent $message" }
        }

        override fun onError(t: Throwable?) {
            logger.warn { "Failed to sent $message. ${t?.message}" }
        }

        override fun onCompleted() {
            // sending completed
        }
    }
}
