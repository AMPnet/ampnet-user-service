package com.ampnet.userservice.grpc

import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.userservice.proto.UserServiceGrpc
import com.ampnet.userservice.proto.UsersResponse
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class GrpcUserServer(private val userRepository: UserRepository) : UserServiceGrpc.UserServiceImplBase() {

    companion object : KLogging()

    override fun getUsers(request: GetUsersRequest, responseObserver: StreamObserver<UsersResponse>) {
        logger.debug { "Received request: GetUsersRequest" }

        val uuids = request.uuidsList.mapNotNull {
            try {
                UUID.fromString(it)
            } catch (ex: IllegalArgumentException) {
                logger.warn(ex.message)
                null
            }
        }
        val users = userRepository.findAllById(uuids)

        val usersResponse = users.map { buildUserResponseFromUser(it) }
        val response = UsersResponse.newBuilder()
            .addAllUsers(usersResponse)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    fun buildUserResponseFromUser(user: User): UserResponse =
        UserResponse.newBuilder()
            .setUuid(user.uuid.toString())
            .setEmail(user.email)
            .setFirstName(user.userInfo.firstName)
            .setLastName(user.userInfo.lastName)
            .setEnabled(user.enabled)
            .build()
}
