package com.ampnet.userservice.grpc

import com.ampnet.userservice.COOP
import com.ampnet.userservice.TestBase
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.proto.GetUserRequest
import com.ampnet.userservice.proto.GetUsersByEmailRequest
import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.userservice.proto.UserWithInfoResponse
import com.ampnet.userservice.proto.UsersResponse
import com.ampnet.userservice.service.AdminService
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.ZonedDateTime
import java.util.Optional
import java.util.UUID

class GrpcUserServerTest : TestBase() {

    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val userInfoRepository = Mockito.mock(UserInfoRepository::class.java)
    private val adminService = Mockito.mock(AdminService::class.java)

    private lateinit var grpcService: GrpcUserServer
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        Mockito.reset(userRepository)
        Mockito.reset(userInfoRepository)
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
            user.role = UserRole.TOKEN_ISSUER
            Mockito.`when`(adminService.changeUserRole(COOP, user.uuid, UserRole.TOKEN_ISSUER)).thenReturn(user)
            grpcService.setUserRole(request, streamObserver)
            val response = grpcService.buildUserResponseFromUser(user)
            Mockito.verify(streamObserver).onNext(response)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    @Test
    fun mustReturnRequestedUser() {
        suppose("User exist") {
            testContext.uuid = UUID.randomUUID()
            testContext.user = createUser(testContext.uuid)
            Mockito.`when`(userRepository.findById(testContext.uuid)).thenReturn(Optional.of(testContext.user))
        }

        verify("Grpc service will return user") {
            val request = GetUserRequest.newBuilder()
                .setUuid(testContext.uuid.toString())
                .build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java) as StreamObserver<UserWithInfoResponse>

            grpcService.getUserWithInfo(request, streamObserver)
            val userResponse = grpcService.buildUserWithInfoResponseFromUser(testContext.user)
            Mockito.verify(streamObserver).onNext(userResponse)
            Mockito.verify(streamObserver).onCompleted()
            Mockito.verify(streamObserver, Mockito.never()).onError(Mockito.any())
        }
    }

    @Test
    fun mustReturnRequestedUsersByEmail() {
        suppose("Users exist") {
            testContext.emails = listOf("user1@email.com", "user2@email.com")
            testContext.users = listOf(
                createUser(UUID.randomUUID(), testContext.emails.first()), createUser(UUID.randomUUID(), testContext.emails.last())
            )
            Mockito.`when`(userRepository.findByCoopAndEmailIn(COOP, testContext.emails)).thenReturn(testContext.users)
        }

        verify("Grpc service will return users") {
            val request = GetUsersByEmailRequest.newBuilder()
                .addAllEmails(testContext.emails)
                .setCoop(COOP)
                .build()

            @Suppress("UNCHECKED_CAST")
            val streamObserver = Mockito.mock(StreamObserver::class.java) as StreamObserver<UsersResponse>

            grpcService.getUsersByEmail(request, streamObserver)
            val usersResponse = testContext.users.map { grpcService.buildUserResponseFromUser(it) }
            val response = UsersResponse.newBuilder().addAllUsers(usersResponse).build()
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

    private fun createUser(
        uuid: UUID,
        email: String = "email@mail.com",
        coop: String = COOP
    ): User =
        User(
            uuid, "first", "last", email, null, AuthMethod.EMAIL,
            null, UserRole.USER, ZonedDateTime.now(), true, coop
        )

    private class TestContext {
        lateinit var uuids: List<UUID>
        lateinit var users: List<User>
        lateinit var uuid: UUID
        lateinit var user: User
        lateinit var emails: List<String>
    }
}
