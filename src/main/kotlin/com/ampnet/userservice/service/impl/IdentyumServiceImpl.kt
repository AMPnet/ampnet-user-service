package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.request.IdentyumPayloadRequest
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.IdentyumException
import com.ampnet.userservice.exception.IdentyumCommunicationException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.persistence.model.UserInfo
import com.ampnet.userservice.persistence.repository.UserInfoRepository
import com.ampnet.userservice.service.IdentyumService
import com.ampnet.userservice.service.pojo.IdentyumUserModel
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class IdentyumServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val userInfoRepository: UserInfoRepository
) : IdentyumService {

    companion object : KLogging()

    private val fieldUsername = "username"
    private val fieldPassword = "password"

    @Transactional(readOnly = true)
    @Throws(IdentyumCommunicationException::class)
    override fun getToken(): String {
        val request = generateIdentyumRequest()
        try {
            val response = restTemplate.postForEntity<String>(applicationProperties.identyum.url, request)
            if (response.statusCode.is2xxSuccessful) {
                response.body?.let {
                    return it
                }
            }
            throw IdentyumCommunicationException(ErrorCode.REG_IDENTYUM_TOKEN,
                    "Could not get Identyum token. Status code: ${response.statusCode.value()}. Body: ${response.body}")
        } catch (ex: RestClientException) {
            throw IdentyumCommunicationException(ErrorCode.REG_IDENTYUM_TOKEN, "Could not reach Identyum", ex)
        }
    }

    @Transactional
    @Throws(IdentyumException::class)
    override fun createUserInfo(request: IdentyumPayloadRequest): UserInfo {
        val decryptedData = decrypt(request.payload, applicationProperties.identyum.key, request.reportUuid)
        val identyumUser: IdentyumUserModel = objectMapper.readValue(decryptedData)

        if (userInfoRepository.findByWebSessionUuid(request.webSessionUuid).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.REG_IDENTYUM_EXISTS,
                "UserInfo with this webSessionUuid already exists! webSessionUuid: ${request.webSessionUuid}")
        }
        val userInfo = createUserInfoFromIdentyumUser(identyumUser)
        userInfo.webSessionUuid = request.webSessionUuid
        return userInfoRepository.save(userInfo)
    }

    @Suppress("TooGenericExceptionCaught")
    fun decrypt(value: String, key: String, reportUuid: String): String {
        try {
            val md = MessageDigest.getInstance("MD5")
            val keyMD5 = md.digest(key.toByteArray())
            val ivMD5 = md.digest(reportUuid.toByteArray())
            val iv = IvParameterSpec(ivMD5)
            val skeySpec = SecretKeySpec(keyMD5, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv)
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(value))
            return String(decrypted)
        } catch (ex: Exception) {
            throw IdentyumException("Could not decode Identyum payload", ex)
        }
    }

    @Suppress("ThrowsCount")
    fun createUserInfoFromIdentyumUser(identyumUser: IdentyumUserModel): UserInfo {
        val userInfo = UserInfo::class.java.getDeclaredConstructor().newInstance()
        val document = identyumUser.document.firstOrNull() ?: throw IdentyumException("Missing document")
        userInfo.apply {
            phoneNumber = identyumUser.phones.firstOrNull()?.phoneNumber ?: throw IdentyumException("Missing phone")
            verifiedEmail = identyumUser.emails.firstOrNull()?.email ?: throw IdentyumException("Missing email")
            country = document.countryCode
            dateOfBirth = document.dateOfBirth
            identyumNumber = identyumUser.identyumUuid
            idType = document.type
            idNumber = document.docNumber
            personalId = document.personalIdentificationNumber.value
            createdAt = ZonedDateTime.now()
            connected = false
        }
        return userInfo
    }

    private fun generateIdentyumRequest(): HttpEntity<MultiValueMap<String, String>> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        val map = LinkedMultiValueMap<String, String>()
        map[fieldUsername] = applicationProperties.identyum.username
        map[fieldPassword] = applicationProperties.identyum.password
        return HttpEntity(map, headers)
    }
}
