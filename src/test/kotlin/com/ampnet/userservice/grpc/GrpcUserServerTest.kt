package com.ampnet.userservice.grpc

import com.ampnet.userservice.TestBase
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.UsersResponse
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.ZonedDateTime
import java.util.UUID

class GrpcUserServerTest : TestBase() {

    private val userRepository = Mockito.mock(UserRepository::class.java)

    private lateinit var grpcService: GrpcUserServer
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        Mockito.reset(userRepository)
        grpcService = GrpcUserServer(userRepository)
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

    private fun createListOfUser(uuid: List<UUID>): List<User> {
        val users = mutableListOf<User>()
        uuid.forEach {
            val user = createUser(it)
            users.add(user)
        }
        return users
    }

    private fun createUser(uuid: UUID): User =
        User::class.java.getConstructor().newInstance().apply {
            authMethod = AuthMethod.EMAIL
            createdAt = ZonedDateTime.now()
            email = "email@mail.com"
            enabled = true
            this.userInfo = createUserInfo()
            this.userInfo.connected = true
            this.uuid = uuid
        }

    private fun createUserInfo(): UserInfo =
        UserInfo::class.java.getDeclaredConstructor().newInstance().apply {
            firstName = "first"
            lastName = "last"
            verifiedEmail = "email@mail.com"
            phoneNumber = "+3859"
            country = "HRV"
            dateOfBirth = "2002-07-01"
            identyumNumber = UUID.randomUUID().toString()
            webSessionUuid = UUID.randomUUID().toString()
            documentType = "ID"
            documentNumber = "1242342"
            citizenship = "HRV"
            resident = true
            addressCity = "city"
            addressCounty = "county"
            addressStreet = "street"
            createdAt = ZonedDateTime.now()
            connected = false
        }

    private class TestContext {
        lateinit var uuids: List<UUID>
        lateinit var users: List<User>
    }
}
