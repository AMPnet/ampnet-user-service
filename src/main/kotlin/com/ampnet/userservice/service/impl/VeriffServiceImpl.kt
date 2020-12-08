package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.exception.VeriffReasonCode
import com.ampnet.userservice.exception.VeriffVerificationCode
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.model.VeriffSession
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.persistence.repository.VeriffSessionRepository
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.VeriffService
import com.ampnet.userservice.service.pojo.ServiceVerificationResponse
import com.ampnet.userservice.service.pojo.VeriffDocument
import com.ampnet.userservice.service.pojo.VeriffPerson
import com.ampnet.userservice.service.pojo.VeriffResponse
import com.ampnet.userservice.service.pojo.VeriffSessionRequest
import com.ampnet.userservice.service.pojo.VeriffSessionResponse
import com.ampnet.userservice.service.pojo.VeriffStatus
import com.ampnet.userservice.service.pojo.VeriffVerification
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.postForEntity
import java.net.URI
import java.security.MessageDigest
import java.util.UUID

@Service
class VeriffServiceImpl(
    private val veriffSessionRepository: VeriffSessionRepository,
    private val userInfoRepository: UserInfoRepository,
    private val applicationProperties: ApplicationProperties,
    private val userService: UserService,
    private val restTemplate: RestTemplate
) : VeriffService {

    companion object : KLogging()

    private val objectMapper: ObjectMapper by lazy {
        val mapper = ObjectMapper()
        mapper.propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.registerModule(KotlinModule())
    }

    @Throws(VeriffException::class)
    override fun getVeriffSession(userUuid: UUID): ServiceVerificationResponse? {
        veriffSessionRepository.findByUserUuidOrderByCreatedAtDesc(userUuid).firstOrNull()?.let { session ->
            getVeriffDecision(session.id)?.let { response ->
                return when (response.verification?.status) {
                    VeriffStatus.approved, VeriffStatus.resubmission_requested ->
                        ServiceVerificationResponse(session.url, response.verification)
                    VeriffStatus.declined, VeriffStatus.abandoned, VeriffStatus.expired -> {
                        createVeriffSession(userUuid)?.let { newSession ->
                            ServiceVerificationResponse(newSession.verification.url, response.verification)
                        }
                    }
                    else -> {
                        // TODO: rethink about this situation
                        logger.warn { "Received unknown status for Veriff verification: ${session.id}. $response" }
                        createVeriffSession(userUuid)?.let { newSession ->
                            ServiceVerificationResponse(newSession.verification.url)
                        }
                    }
                }
            }
        }
        return createVeriffSession(userUuid)?.let { newSession ->
            ServiceVerificationResponse(newSession.verification.url)
        }
    }

    @Throws(VeriffException::class)
    override fun saveUserVerificationData(data: String): UserInfo? {
        val response = mapVeriffResponse(data)
        val verification = response.verification
            ?: throw VeriffException("Missing verification data. Status: ${response.status}")
        getVeriffPerson(verification)?.let { person ->
            val document = getVeriffDocument(verification)
            val userInfo = UserInfo(verification.id, person, document)
            userInfoRepository.save(userInfo)
            logger.info { "Successfully created user info: ${userInfo.uuid}" }
            verifyUser(userInfo, verification.vendorData)
            return userInfo
        }
        return null
    }

    @Throws(VeriffException::class)
    override fun verifyClient(client: String) {
        if (client != applicationProperties.veriff.apiKey) {
            throw VeriffException("X-AUTH-CLIENT: $client is invalid")
        }
    }

    @Throws(VeriffException::class)
    override fun verifySignature(signature: String, data: String) {
        val hexHash = generateSignature(data)
        if (signature != hexHash) {
            logger.error { "Invalid Veriff signature!" }
            // throw VeriffException("Invalid signature!")
        }
    }

    private fun createVeriffSession(userUuid: UUID): VeriffSessionResponse? {
        val user = userService.find(userUuid)
            ?: throw ResourceNotFoundException(ErrorCode.USER_MISSING, "Missing user: $userUuid")
        val callback = "" // TODO: set callback
        val request = objectMapper.writeValueAsString(VeriffSessionRequest(user, callback))
        val signature = generateSignature(request)
        val headers = generateVeriffHeaders(signature)
        val httpEntity = HttpEntity(request, headers)
        val uri = URI(applicationProperties.veriff.baseUrl + "/v1/sessions/")
        return try {
            val veriffSessionResponse = restTemplate.postForEntity<VeriffSessionResponse>(uri, httpEntity).body
            veriffSessionResponse?.let {
                val veriffSession = VeriffSession(veriffSessionResponse, userUuid)
                veriffSessionRepository.save(veriffSession)
            }
            return veriffSessionResponse
        } catch (ex: RestClientException) {
            logger.warn("Could not create Veriff session", ex)
            null
        }
    }

    private fun getVeriffDecision(sessionId: String): VeriffResponse? {
        val signature = generateSignature(sessionId)
        val headers = generateVeriffHeaders(signature)
        val httpEntity = HttpEntity<String>(headers)
        val uri = URI(applicationProperties.veriff.baseUrl + "/v1/sessions/$sessionId/decision")
        return try {
            val response = restTemplate.exchange<String>(uri, HttpMethod.GET, httpEntity).body
            response?.let {
                return mapVeriffResponse(response)
            }
            return null
        } catch (ex: RestClientException) {
            logger.warn("Could not get Veriff decision", ex)
            null
        }
    }

    private fun generateSignature(payload: String): String {
        val request = payload + applicationProperties.veriff.privateKey
        val hash = MessageDigest.getInstance("SHA-256").digest(request.toByteArray())
        return bytesToHex(hash)
    }

    private fun generateVeriffHeaders(signature: String): HttpHeaders {
        val headers = HttpHeaders()
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        headers.set("X-AUTH-CLIENT", applicationProperties.veriff.apiKey)
        headers.set("X-SIGNATURE", signature)
        return headers
    }

    private fun verifyUser(userInfo: UserInfo, vendorData: String?) {
        vendorData?.let {
            try {
                val userUuid = UUID.fromString(it)
                userService.connectUserInfo(userUuid, userInfo.sessionId)
                connectVeriffSession(userInfo.sessionId)
            } catch (ex: IllegalArgumentException) {
                logger.warn("Vendor data: $it is not in valid format", ex)
            }
        }
    }

    private fun connectVeriffSession(sessionId: String) {
        ServiceUtils.wrapOptional(veriffSessionRepository.findById(sessionId))?.let {
            it.connected = true
        }
    }

    private fun getVeriffPerson(verification: VeriffVerification): VeriffPerson? {
        val status = verification.status
        val code = VeriffVerificationCode.fromInt(verification.code)
        val reason = VeriffReasonCode.fromInt(verification.reasonCode)
        val message = "Status: $status with code ${code?.code} - ${code?.name}. " +
            "Reason code: ${reason?.code} - ${reason?.name}. Reason: ${verification.reason}. " +
            "Session id: ${verification.id}"
        if (status != VeriffStatus.approved) {
            logger.info { "Verification not approved. $message" }
            return null
        }
        return verification.person
            ?: throw VeriffException("Missing verification person data. $message")
    }

    private fun getVeriffDocument(verification: VeriffVerification): VeriffDocument {
        if (verification.document == null) {
            val reason = VeriffReasonCode.fromInt(verification.reasonCode)
            throw VeriffException(
                "Missing document. Reason: ${reason?.code} - ${reason?.name}. Session id: ${verification.id}"
            )
        }
        return verification.document
    }

    private fun mapVeriffResponse(response: String): VeriffResponse =
        try {
            objectMapper.readValue(response)
        } catch (ex: JsonProcessingException) {
            throw VeriffException("Could not map Veriff response", ex)
        }

    @Suppress("MagicNumber")
    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (i in hash.indices) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}
