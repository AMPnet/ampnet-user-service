package com.ampnet.userservice.service

import com.ampnet.userservice.COOP
import com.ampnet.userservice.TestBase
import com.ampnet.userservice.config.DatabaseCleanerService
import com.ampnet.userservice.config.PasswordEncoderConfig
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.grpc.mailservice.MailService
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.model.Document
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.persistence.repository.ForgotPasswordTokenRepository
import com.ampnet.userservice.persistence.repository.MailTokenRepository
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@DataJpaTest
@Transactional
@Import(DatabaseCleanerService::class, PasswordEncoderConfig::class)
abstract class JpaServiceTestBase : TestBase() {

    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var userRepository: UserRepository

    @Autowired
    protected lateinit var mailTokenRepository: MailTokenRepository

    @Autowired
    protected lateinit var forgotPasswordTokenRepository: ForgotPasswordTokenRepository

    @Autowired
    protected lateinit var userInfoRepository: UserInfoRepository

    @Autowired
    protected lateinit var coopRepository: CoopRepository

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected val mailService: MailService = Mockito.mock(MailService::class.java)
    protected val cloudStorageService: CloudStorageService = Mockito.mock(CloudStorageService::class.java)

    protected fun createUser(
        email: String,
        firstName: String = "first",
        lastName: String = "last",
        password: String? = null,
        authMethod: AuthMethod = AuthMethod.EMAIL,
        coop: String = COOP
    ): User {
        val user = User(
            UUID.randomUUID(),
            firstName,
            lastName,
            email,
            password,
            authMethod,
            null,
            UserRole.USER,
            ZonedDateTime.now(),
            true,
            coop
        )
        return userRepository.save(user)
    }

    protected fun createUserInfo(
        sessionId: String = UUID.randomUUID().toString(),
        first: String = "firstname",
        last: String = "lastname",
        email: String = "email@mail.com",
        disabled: Boolean = false
    ): UserInfo {
        val userInfo = UserInfo::class.java.getDeclaredConstructor().newInstance().apply {
            this.sessionId = sessionId
            firstName = first
            lastName = last
            dateOfBirth = "1911-07-01"
            document = Document("ID_CARD", "12345678", "2020-02-02", "HRV", "1939-09-01")
            nationality = "HRV"
            placeOfBirth = "City, address"
            createdAt = ZonedDateTime.now()
            connected = false
            this.deactivated = disabled
        }
        return userInfoRepository.save(userInfo)
    }

    protected fun setUserInfo(user: User, userInfoId: Int) {
        user.userInfoId = userInfoId
        userRepository.save(user)
    }

    protected fun createCoop(identifier: String = COOP, link: String = "link"): Coop =
        coopRepository.save(Coop(identifier, identifier, "hostname", null, link))
}
