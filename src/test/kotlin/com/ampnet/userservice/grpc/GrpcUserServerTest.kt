package com.ampnet.userservice.grpc

import com.ampnet.userservice.COOP
import com.ampnet.userservice.TestBase
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.persistence.model.Role
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.userservice.proto.UsersResponse
import com.ampnet.userservice.service.AdminService
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.ZonedDateTime
import java.util.UUID

class GrpcUserServerTest : TestBase() {

    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val adminService = Mockito.mock(AdminService::class.java)

    private lateinit var grpcService: GrpcUserServer
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        Mockito.reset(userRepository)
        Mockito.reset(adminService)
        grpcService = GrpcUserServer(userRepository, adminService)
        testContext = TestContext()
    }

    @Test
    fun mustReturnRequestedUsers() {
        suppose("Users exist") {
            testContext.uuids = listOf(UUID.randomUUID(), UUID.randomUUID())
            testContext.users = createListOfUser(testContext.uuids)
            Mockito.`when`(userRepository.findAllById(testContext.uuids)).thenReturn(testContext.users)
        }

        verify("Grpc service will return users") {
            val request = GetUsersRequest.newBuilder()
                .addAllUuids(testContext.uuids.map { it.toString() })
                .build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java) as StreamObserver<UsersResponse>

            grpcService.getUsers(request, streamObserver)
            val usersResponse = testContext.users.map { grpcService.buildUserResponseFromUser(it) }
            val response = UsersResponse.newBuilder().addAllUsers(usersResponse).build()
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    @Test
    fun mustNotFailOnInvalidUuid() {
        verify("Grpc service will not fail on invalid UUID") {
            val request = GetUsersRequest.newBuilder()
                .addUuids("invalid-uuid")
                .build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java) as StreamObserver<UsersResponse>

            grpcService.getUsers(request, streamObserver)
            val response = UsersResponse.newBuilder().clearUsers().build()
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    @Test
    fun mustChangeUserRole() {
        verify("Grpc service will change user role") {
            val user = createUser(UUID.randomUUID())
            val request = SetRoleRequest.newBuilder()
                .setUuid(user.uuid.toString())
                .setRole(SetRoleRequest.Role.TOKEN_ISSUER)
                .setCoop(COOP)
                .build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java) as StreamObserver<UserResponse>

            user.role = Role(0, "TOKEN_ISSUER", "Descr")
            Mockito.`when`(adminService.changeUserRole(user.uuid, UserRoleType.TOKEN_ISSUER, COOP)).thenReturn(user)
            grpcService.setUserRole(request, streamObserver)
            val response = grpcService.buildUserResponseFromUser(user)
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    private fun createListOfUser(uuid: List<UUID>): List<User> {
        val users = mutableListOf<User>()
        uuid.forEach {
            val user = createUser(it)
            users.add(user)
        }
        return users
    }

    private fun createUser(uuid: UUID): User =
        User(
            uuid,
            "first",
            "last",
            "email@mail.com",
            null,
            AuthMethod.EMAIL,
            null,
            Role(0, "USER", "Description"),
            ZonedDateTime.now(),
            true,
            COOP
        )

    private class TestContext {
        lateinit var uuids: List<UUID>
        lateinit var users: List<User>
    }
}
