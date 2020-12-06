package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.VeriffException
import com.ampnet.userservice.exception.VeriffReasonCode
import com.ampnet.userservice.exception.VeriffVerificationCode
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.service.UserService
import com.ampnet.userservice.service.VeriffService
import com.ampnet.userservice.service.pojo.VeriffDocument
import com.ampnet.userservice.service.pojo.VeriffPerson
import com.ampnet.userservice.service.pojo.VeriffResponse
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
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.UUID

@Service
class VeriffServiceImpl(
    private val userInfoRepository: UserInfoRepository,
    private val applicationProperties: ApplicationProperties,
    private val userService: UserService
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
        val request = data + applicationProperties.veriff.privateKey
        val hash = MessageDigest.getInstance("SHA-256").digest(request.toByteArray())
        val hexHash = bytesToHex(hash)
        if (signature != hexHash) {
            logger.error { "Invalid Veriff signature!" }
            // throw VeriffException("Invalid signature!")
        }
    }

    private fun verifyUser(userInfo: UserInfo, vendorData: String?) {
        vendorData?.let {
            try {
                val userUuid = UUID.fromString(it)
                userService.connectUserInfo(userUuid, userInfo.sessionId)
            } catch (ex: IllegalArgumentException) {
                logger.warn("Vendor data: $it is not in valid format", ex)
            }
        }
    }

    private fun getVeriffPerson(verification: VeriffVerification): VeriffPerson? {
        val status = verification.status
        val code = VeriffVerificationCode.fromInt(verification.code)
        val reason = VeriffReasonCode.fromInt(verification.reasonCode)
        val message = "Status: $status with code ${code?.code} - ${code?.name}. " +
            "Reason code: ${reason?.code} - ${reason?.name}. Reason: ${verification.reason}. " +
            "Session id: ${verification.id}"
        if (status != "approved") {
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
