package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.IdentyumCommunicationException
import com.ampnet.userservice.exception.IdentyumException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.service.IdentyumService
import com.ampnet.userservice.service.pojo.IdentyumInput
import com.ampnet.userservice.service.pojo.IdentyumTokenRequest
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Service
class IdentyumServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val userInfoRepository: UserInfoRepository
) : IdentyumService {

    companion object : KLogging()

    private val identyumPublicKey: PublicKey by lazy {
        val pureKey = applicationProperties.identyum.publicKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        try {
            val encoded: ByteArray = decodeBase64(pureKey, "Identyum public key from application.properties")
            val kf: KeyFactory = KeyFactory.getInstance("RSA")
            kf.generatePublic(X509EncodedKeySpec(encoded))
        } catch (ex: GeneralSecurityException) {
            throw IdentyumException("Could not load identyum public key!", ex)
        }
    }
    private val ampnetPrivateKey: PrivateKey by lazy {
        val pureKey = applicationProperties.identyum.ampnetPrivateKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        try {
            val encoded: ByteArray = decodeBase64(pureKey, "ampnet private key from application.properties")
            val kf = KeyFactory.getInstance("RSA")
            val keySpec = PKCS8EncodedKeySpec(encoded)
            kf.generatePrivate(keySpec)
        } catch (ex: GeneralSecurityException) {
            throw IdentyumException("Could not load ampent private key!", ex)
        }
    }

    @Transactional(readOnly = true)
    @Throws(IdentyumCommunicationException::class)
    override fun getToken(): String {
        val request = IdentyumTokenRequest(
            applicationProperties.identyum.username,
            applicationProperties.identyum.password
        )
        try {
            val response = restTemplate
                .postForEntity<String>(applicationProperties.identyum.url, request)
            if (response.statusCode.is2xxSuccessful) {
                response.body?.let {
                    return it
                }
            }
            throw IdentyumCommunicationException(ErrorCode.REG_IDENTYUM_TOKEN,
                    "Could not get Identyum token. Identyum username: ${request.username}" +
                        "Status code: ${response.statusCode.value()}. Body: ${response.body}")
        } catch (ex: RestClientException) {
            throw IdentyumCommunicationException(ErrorCode.REG_IDENTYUM_TOKEN,
                "Could not reach Identyum. Identyum username: ${request.username}", ex)
        }
    }

    @Transactional
    @Throws(IdentyumException::class)
    override fun createUserInfo(report: String, secretKey: String, signature: String): UserInfo {
        val decryptedReport = decryptReport(report, secretKey, signature)
        try {
            val identyumInput: IdentyumInput = mapReport(decryptedReport)
            if (userInfoRepository.findByUserSessionUuid(identyumInput.userSessionUuid.toString()).isPresent) {
                throw ResourceAlreadyExistsException(ErrorCode.REG_IDENTYUM_EXISTS,
                    "UserInfo with UserSessionUuid: ${identyumInput.userSessionUuid} already exists!")
            }
            val userInfo = UserInfo(identyumInput)
            return userInfoRepository.save(userInfo)
        } catch (ex: JsonProcessingException) {
            val trimmedDecryptedReport = removeImages(decryptedReport)
            logger.warn { "Identyum decrypted data: $trimmedDecryptedReport" }
            when (ex) {
                is JsonMappingException ->
                    throw IdentyumException("JSON structured not in defined format, missing some filed", ex)
                is JsonParseException -> throw IdentyumException("Content not in valid JSON format", ex)
                else -> throw IdentyumException("Cannot parse decrypted data", ex)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun findUserInfo(webSessionUuid: String): UserInfo? =
        ServiceUtils.wrapOptional(userInfoRepository.findByUserSessionUuid(webSessionUuid))

    @Throws(IdentyumException::class)
    internal fun decryptReport(report: String, secretKey: String, signature: String): String {
        verifySignature(report, signature)
        val decryptedSecretKey = decryptReportSecretKey(secretKey)
        return decryptRequest(report, decryptedSecretKey)
    }

    internal fun removeImages(jsonString: String): String {
        val jsonNode = objectMapper.readTree(jsonString)
        if (jsonNode is ObjectNode) {
            jsonNode.remove("images")
        }
        return objectMapper.writeValueAsString(jsonNode)
    }

    internal fun mapReport(report: String): IdentyumInput = getIdentyumObjectMapper().readValue(report)

    private fun verifySignature(report: String, signature: String) {
        try {
            val publicSignature: Signature = Signature.getInstance("SHA256withRSA")
            publicSignature.initVerify(identyumPublicKey)
            publicSignature.update(report.toByteArray())

            val signatureBytes: ByteArray = decodeBase64(signature, "Identyum signature")
            val isValid = publicSignature.verify(signatureBytes)
            if (isValid.not()) {
                throw IdentyumException("Invalid Identyum signature")
            }
        } catch (ex: GeneralSecurityException) {
            throw IdentyumException("Could not verify report signature", ex)
        }
    }

    private fun decryptReportSecretKey(encryptedKey: String): SecretKey {
        try {
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.PRIVATE_KEY, ampnetPrivateKey)
            val decodedEncryptedKey = decodeBase64(encryptedKey, "Identyum encrypted key")
            val decryptedKey = cipher.doFinal(decodedEncryptedKey)
            return SecretKeySpec(decryptedKey, 0, decryptedKey.size, "AES")
        } catch (ex: GeneralSecurityException) {
            throw IdentyumException("Could not decrypt report secret key", ex)
        }
    }

    private fun decryptRequest(report: String, secretKey: SecretKey): String {
        try {
            val aesCipher = Cipher.getInstance("AES")
            aesCipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decryptedReport: ByteArray = decodeBase64(report, "Identyum encrypted report")
            val bytePlainText = aesCipher.doFinal(decryptedReport)
            return String(bytePlainText)
        } catch (ex: GeneralSecurityException) {
            throw IdentyumException("Could not decrypt report", ex)
        }
    }

    private fun decodeBase64(data: String, description: String): ByteArray =
        try {
            Base64.getDecoder().decode(data)
        } catch (ex: IllegalArgumentException) {
            throw IdentyumException("Could not decode Base64 data $description", ex)
        }

    private fun getIdentyumObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.registerModule(KotlinModule())
    }
}
