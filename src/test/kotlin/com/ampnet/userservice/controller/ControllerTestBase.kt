package com.ampnet.userservice.controller

import com.ampnet.userservice.COOP
import com.ampnet.userservice.TestBase
import com.ampnet.userservice.config.DatabaseCleanerService
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ErrorResponse
import com.ampnet.userservice.grpc.mailservice.MailService
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.model.Document
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.UserRepository
import com.ampnet.userservice.service.CloudStorageService
import com.ampnet.userservice.service.ReCaptchaService
import com.ampnet.userservice.service.SocialService
import com.ampnet.userservice.service.pojo.SocialUser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
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
    protected lateinit var userInfoRepository: UserInfoRepository

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var coopRepository: CoopRepository

    @MockBean
    protected lateinit var mailService: MailService

    @MockBean
    protected lateinit var socialService: SocialService

    @MockBean
    protected lateinit var reCaptchaService: ReCaptchaService

    @MockBean
    protected lateinit var cloudStorageService: CloudStorageService

    @Autowired
    protected lateinit var cacheManager: CacheManager

    protected lateinit var mockMvc: MockMvc

    @BeforeEach
    fun init(wac: WebApplicationContext, restDocumentation: RestDocumentationContextProvider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .alwaysDo<DefaultMockMvcBuilder>(
                MockMvcRestDocumentation.document(
                    "{ClassName}/{methodName}",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
                )
            )
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
        role: UserRole = UserRole.USER,
        coop: String = COOP
    ): User {
        val user = User(
            uuid,
            "firstname",
            "lastname",
            email,
            passwordEncoder.encode(password.orEmpty()),
            auth,
            null,
            role,
            ZonedDateTime.now(),
            true,
            coop
        )
        return userRepository.save(user)
    }

    protected fun createUserInfo(
        first: String = "firstname",
        last: String = "lastname",
        email: String = "email@mail.com",
        phone: String = "+3859",
        sessionId: String = UUID.randomUUID().toString(),
        connected: Boolean = true,
        disabled: Boolean = false
    ): UserInfo {
        val userInfo = UserInfo(
            UUID.randomUUID(),
            sessionId,
            first,
            last,
            "id-number",
            "1911-07-01",
            Document("ID_CARD", "12345678", "2020-02-02", "HRV", "1939-09-01"),
            "HRV",
            "Place",
            ZonedDateTime.now(),
            connected,
            disabled
        )
        return userInfoRepository.save(userInfo)
    }

    protected fun generateSocialUser(email: String, first: String = "First", last: String = "Last") =
        SocialUser(email, first, last)

    protected fun createCoop(identifier: String = COOP, config: String? = null, link: String = "link"): Coop =
        coopRepository.save(Coop(identifier, identifier, "host.com", config, link))

    protected fun serializeConfig(config: Map<String, Any>?): String = objectMapper.writeValueAsString(config)

    protected data class CoopResponseTest(
        val identifier: String,
        val name: String,
        val createdAt: ZonedDateTime,
        val hostname: String,
        val config: Map<String, Any>?,
        val logo: String,
        val needUserVerification: Boolean
    )
}
