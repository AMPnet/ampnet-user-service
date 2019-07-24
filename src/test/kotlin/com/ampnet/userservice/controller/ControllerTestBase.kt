package com.ampnet.userservice.controller

import com.ampnet.userservice.TestBase
import com.ampnet.userservice.config.DatabaseCleanerService
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ErrorResponse
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.RoleRepository
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import java.time.ZonedDateTime
import java.util.UUID

@ExtendWith(value = [SpringExtension::class, RestDocumentationExtension::class])
@SpringBootTest
@ActiveProfiles("MailMockConfig, BlockchainServiceMockConfig, CloudStorageMockConfig")
abstract class ControllerTestBase : TestBase() {

    protected val defaultEmail = "user@email.com"

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

    protected fun createUser(email: String, auth: AuthMethod = AuthMethod.EMAIL, password: String? = null): User {
        return createUser(email, auth, password, createUserInfo())
    }

    protected fun createUser(email: String, auth: AuthMethod, password: String?, userInfo: UserInfo): User {
        val user = User::class.java.getConstructor().newInstance().apply {
            authMethod = auth
            createdAt = ZonedDateTime.now()
            this.email = email
            enabled = true
            role = roleRepository.getOne(UserRoleType.USER.id)
            this.userInfo = userInfo
            uuid = UUID.randomUUID()
            this.password = passwordEncoder.encode(password.orEmpty())
        }
        return userRepository.save(user)
    }

    protected fun createUserInfo(
        first: String = "firstname",
        last: String = "lastname",
        email: String = "email@mail.com",
        phone: String = "+3859",
        webSessionUuid: String = "1234-1234-1234-1234"
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
            connected = true
        }
        return userInfoRepository.save(userInfo)
    }
}
