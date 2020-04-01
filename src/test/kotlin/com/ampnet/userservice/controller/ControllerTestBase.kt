package com.ampnet.userservice.controller

import com.ampnet.userservice.TestBase
import com.ampnet.userservice.config.DatabaseCleanerService
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ErrorResponse
import com.ampnet.userservice.grpc.mailservice.MailService
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.RoleRepository
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.pojo.SocialUser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
@ActiveProfiles("MailMockConfig, BlockchainServiceMockConfig, CloudStorageMockConfig")
abstract class ControllerTestBase : TestBase() {

    protected val defaultEmail = "user@email.com"
    protected val defaultUuid: UUID = UUID.fromString("8a733721-9bb3-48b1-90b9-6463ac1493eb")

    @Autowired
    protected lateinit var objectMapper: ObjectMapper
    @Autowired
    protected lateinit var databaseCleanerService: DatabaseCleanerService
    @Autowired
    protected lateinit var userRepository: UserRepository
    @Autowired
    protected lateinit var roleRepository: RoleRepository
    @Autowired
    protected lateinit var userInfoRepository: UserInfoRepository
    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder
    @Autowired
    protected lateinit var mailService: MailService

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun init(wac: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
                .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
                .alwaysDo<DefaultMockMvcBuilder>(MockMvcRestDocumentation.document(
                        "{ClassName}/{methodName}",
                        Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                        Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                ))
                .build()
    }

    protected fun getResponseErrorCode(errorCode: ErrorCode): String {
        return errorCode.categoryCode + errorCode.specificCode
    }

    protected fun verifyResponseErrorCode(result: MvcResult, errorCode: ErrorCode) {
        val response: ErrorResponse = objectMapper.readValue(result.response.contentAsString)
        val expectedErrorCode = getResponseErrorCode(errorCode)
        assert(response.errCode == expectedErrorCode)
    }

    protected fun createUser(
        email: String,
        auth: AuthMethod = AuthMethod.EMAIL,
        password: String? = null,
        uuid: UUID = UUID.randomUUID(),
        role: UserRoleType = UserRoleType.USER
    ): User {
        val user = User(
            uuid,
            "firstname",
            "lastname",
            email,
            passwordEncoder.encode(password.orEmpty()),
            auth,
            null,
            roleRepository.getOne(role.id),
            ZonedDateTime.now(),
            true
        )
        return userRepository.save(user)
    }

    protected fun createUserInfo(
        first: String = "firstname",
        last: String = "lastname",
        email: String = "email@mail.com",
        phone: String = "+3859",
        webSessionUuid: String = "1234-1234-1234-1234",
        connected: Boolean = true,
        disabled: Boolean = false
    ): UserInfo {
        val userInfo = UserInfo::class.java.getDeclaredConstructor().newInstance().apply {
            this.webSessionUuid = webSessionUuid
            firstName = first
            lastName = last
            verifiedEmail = email
            phoneNumber = phone
            country = "HRV"
            dateOfBirth = "2002-07-01"
            identyumNumber = UUID.randomUUID().toString()
            documentType = "ID"
            documentNumber = "1242342"
            citizenship = "HRV"
            resident = true
            addressCity = "city"
            addressCounty = "county"
            addressStreet = "street"
            createdAt = ZonedDateTime.now()
            this.connected = connected
            this.deactivated = disabled
        }
        return userInfoRepository.save(userInfo)
    }

    protected fun generateSocialUser(email: String, first: String = "First", last: String = "Last") =
        SocialUser(email, first, last)
}
