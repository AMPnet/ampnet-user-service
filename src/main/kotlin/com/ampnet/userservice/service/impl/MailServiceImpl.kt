package com.ampnet.userservice.service.impl

import com.ampnet.mailservice.proto.Empty
import com.ampnet.mailservice.proto.MailServiceGrpc
import com.ampnet.mailservice.proto.MailConfirmationRequest
import com.ampnet.userservice.service.MailService
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

@Service
class MailServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory
) : MailService {

    companion object : KLogging()

    private val mailServiceStub: MailServiceGrpc.MailServiceStub by lazy {
        val channel = grpcChannelFactory.createChannel("mail-service")
        MailServiceGrpc.newStub(channel)
    }

    override fun sendConfirmationMail(email: String, token: String) {
        logger.debug { "Sending confirmation mail to: $email" }
        val request = MailConfirmationRequest.newBuilder()
            .setEmail(email)
            .setToken(token)
            .build()

        mailServiceStub.sendMailConfirmation(request, object : StreamObserver<Empty> {
            override fun onNext(value: Empty?) {
                logger.info { "Successfully sent confirmation mail to: $email" }
            }

            override fun onError(t: Throwable?) {
                logger.warn { "Failed to sent confirmation mail to: $email. ${t?.message}" }
            }

            override fun onCompleted() {
                // sending completed
            }
        })
    }
}
