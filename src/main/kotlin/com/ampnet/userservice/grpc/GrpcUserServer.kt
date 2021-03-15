package com.ampnet.userservice.grpc

import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InvalidRequestException
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.proto.CoopRequest
import com.ampnet.userservice.proto.CoopResponse
import com.ampnet.userservice.proto.GetUserRequest
import com.ampnet.userservice.proto.GetUsersByEmailRequest
import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.Role
import com.ampnet.userservice.proto.SetRoleRequest
import com.ampnet.userservice.proto.UserExtendedResponse
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.userservice.proto.UserServiceGrpc
import com.ampnet.userservice.proto.UserWithInfoResponse
import com.ampnet.userservice.proto.UsersExtendedResponse
import com.ampnet.userservice.proto.UsersResponse
import com.ampnet.userservice.service.AdminService
import com.ampnet.userservice.service.CoopService
import com.ampnet.userservice.service.impl.ServiceUtils
import com.ampnet.userservice.service.pojo.CoopServiceResponse
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.server.service.GrpcService
import java.util.UUID

@GrpcService
class GrpcUserServer(
    private val userRepository: UserRepository,
    private val adminService: AdminService,
    private val coopService: CoopService,
    private val userInfoRepository: UserInfoRepository
) : UserServiceGrpc.UserServiceImplBase() {

    companion object : KLogging()

    override fun getUsers(request: GetUsersRequest, responseObserver: StreamObserver<UsersResponse>) {
        logger.debug { "Received gRPC request: GetUsersRequest" }
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

    override fun setUserRole(request: SetRoleRequest, responseObserver: StreamObserver<UserResponse>) {
        logger.info { "Received gRPC request setUserRole: $request" }
        try {
            val userUuid = UUID.fromString(request.uuid)
            val role = getRole(request.role)
            val user = adminService.changeUserRole(request.coop, userUuid, role)
            logger.info { "Successfully set new role: $role to user: ${user.uuid}" }
            responseObserver.onNext(buildUserResponseFromUser(user))
            responseObserver.onCompleted()
        } catch (ex: IllegalArgumentException) {
            logger.warn(ex) { "Could not get new user role values" }
            responseObserver.onError(ex)
        } catch (ex: InvalidRequestException) {
            logger.warn(ex) { "Missing user to change his role" }
            responseObserver.onError(ex)
        }
    }

    override fun getPlatformManagers(request: CoopRequest, responseObserver: StreamObserver<UsersResponse>) {
        logger.debug { "Received gRPC request getPlatformManagers: $request" }
        val platformManagers = adminService
            .findByRoles(request.coop, listOf(UserRole.ADMIN, UserRole.PLATFORM_MANAGER))
            .map { buildUserResponseFromUser(it) }
        val response = UsersResponse.newBuilder()
            .addAllUsers(platformManagers)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getTokenIssuers(request: CoopRequest, responseObserver: StreamObserver<UsersResponse>) {
        logger.debug { "Received gRPC request getTokenIssuers: $request" }
        val tokenIssuers = adminService
            .findByRoles(request.coop, listOf(UserRole.ADMIN, UserRole.TOKEN_ISSUER))
            .map { buildUserResponseFromUser(it) }
        val response = UsersResponse.newBuilder()
            .addAllUsers(tokenIssuers)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getUserWithInfo(request: GetUserRequest, responseObserver: StreamObserver<UserWithInfoResponse>) {
        logger.debug { "Received gRPC request getUserWithInfo: $request" }
        try {
            val user = ServiceUtils.wrapOptional(userRepository.findById(UUID.fromString(request.uuid)))
                ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "Missing user with uuid: $request.uuid")
            val coop = coopService.getCoopByIdentifier(user.coop)
                ?: throw ResourceNotFoundException(ErrorCode.COOP_MISSING, "Missing coop: ${user.coop} on platform")
            responseObserver.onNext(buildUserWithInfoResponseFromUser(user, coop))
            responseObserver.onCompleted()
        } catch (ex: ResourceNotFoundException) {
            logger.warn(ex) { "Could not get userWithInfo" }
            responseObserver.onError(ex)
        }
    }

    override fun getUsersByEmail(request: GetUsersByEmailRequest, responseObserver: StreamObserver<UsersResponse>) {
        logger.debug {
            "Received gRPC request getUsersByEmail for emails: " +
                "${request.emailsList.joinToString()} and coop: ${request.coop}"
        }
        val users = userRepository.findByCoopAndEmailIn(request.coop, request.emailsList)
        val usersResponse = users.map { buildUserResponseFromUser(it) }
        val response = UsersResponse.newBuilder()
            .addAllUsers(usersResponse)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getAllActiveUsers(
        request: CoopRequest,
        responseObserver: StreamObserver<UsersExtendedResponse>
    ) {
        logger.debug { "Received gRPC request getAllActiveUsers for coop: ${request.coop}" }
        try {
            val coop = coopService.getCoopByIdentifier(request.coop)
                ?: throw ResourceNotFoundException(ErrorCode.COOP_MISSING, "Missing coop: ${request.coop} on platform")
            val users = userRepository.findAllByCoopAndUserInfoUuidIsNotNull(request.coop)
                .associateBy { it.userInfoUuid }
            val usersWithExtendedInfo = userInfoRepository.findAllByCoop(request.coop).map { userInfo ->
                users[userInfo.uuid]?.let { user -> buildUserExtendedResponse(user, userInfo) }
            }
            val response = UsersExtendedResponse.newBuilder()
                .addAllUsers(usersWithExtendedInfo)
                .setCoop(buildCoopResponse(coop))
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (ex: ResourceNotFoundException) {
            logger.warn(ex) { "Could not get coop: ${request.coop}" }
            responseObserver.onError(ex)
        }
    }

    private fun getRole(role: Role): UserRole =
        when (role) {
            Role.ADMIN -> UserRole.ADMIN
            Role.PLATFORM_MANAGER -> UserRole.PLATFORM_MANAGER
            Role.TOKEN_ISSUER -> UserRole.TOKEN_ISSUER
            Role.USER -> UserRole.USER
            else -> throw IllegalArgumentException("Invalid user role")
        }

    internal fun buildUserResponseFromUser(user: User): UserResponse =
        UserResponse.newBuilder()
            .setUuid(user.uuid.toString())
            .setEmail(user.email)
            .setFirstName(user.firstName)
            .setLastName(user.lastName)
            .setEnabled(user.enabled)
            .setCoop(user.coop)
            .setLanguage(user.language.orEmpty())
            .build()

    internal fun buildUserWithInfoResponseFromUser(user: User, coop: CoopServiceResponse): UserWithInfoResponse {
        val builder = UserWithInfoResponse.newBuilder()
            .setUser(buildUserResponseFromUser(user))
            .setCreatedAt(user.createdAt.toInstant().toEpochMilli())
            .setCoop(buildCoopResponse(coop))
        return builder.build()
    }

    internal fun buildUserExtendedResponse(user: User, userInfo: UserInfo): UserExtendedResponse =
        UserExtendedResponse.newBuilder()
            .setUuid(user.uuid.toString())
            .setFirstName(userInfo.firstName)
            .setLastName(userInfo.lastName)
            .setCreatedAt(userInfo.createdAt.toInstant().toEpochMilli())
            .setLanguage(user.language.orEmpty())
            .setDateOfBirth(userInfo.dateOfBirth.orEmpty())
            .setDocumentNumber(userInfo.document.number.orEmpty())
            .setDateOfIssue(userInfo.document.validFrom.orEmpty())
            .setDateOfExpiry(userInfo.document.validUntil.orEmpty())
            .setPersonalNumber(userInfo.idNumber.orEmpty())
            .build()

    internal fun buildCoopResponse(coop: CoopServiceResponse): CoopResponse =
        CoopResponse.newBuilder()
            .setCoop(coop.identifier)
            .setName(coop.name)
            .setHostname(coop.hostname.orEmpty())
            .setLogo(coop.logo.orEmpty())
            .build()
}
