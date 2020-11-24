package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ReCaptchaException
import com.ampnet.userservice.service.ReCaptchaService
import com.ampnet.userservice.service.pojo.GoogleResponse
import com.ampnet.userservice.service.pojo.ReCaptchaRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

@Service
class ReCaptchaServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate
) : ReCaptchaService {

    override fun processResponseToken(reCaptchaToken: String, remoteIp: String) {
        if (!applicationProperties.reCaptcha.enabled) return
        val request = ReCaptchaRequest(
            applicationProperties.reCaptcha.secret,
            reCaptchaToken,
            remoteIp
        )
        try {
            val responseEntity = restTemplate.postForEntity<String>(
                applicationProperties.reCaptcha.url, request
            )
            val body = responseEntity.body
                ?: throwReCaptchaException("ReCAPTCHA verification failed, empty response from Google's server")
            val googleResponse: GoogleResponse = GoogleResponse.fromJson(body)
            if (responseEntity.statusCode.is2xxSuccessful) {
                if (googleResponse.success) {
                    validateScore(googleResponse)
                    return
                }
            }
            val joinedErrors = googleResponse.errorCodes.joinToString()
            throwReCaptchaException("ReCAPTCHA verification failed", mapOf("errors" to joinedErrors))
        } catch (ex: RestClientException) {
            throwReCaptchaException(
                "ReCAPTCHA verification failed, error response from Google's server", throwable = ex
            )
        }
    }

    fun validateScore(googleResponse: GoogleResponse) {
        if (googleResponse.score < applicationProperties.reCaptcha.score) {
            throwReCaptchaException(
                "ReCAPTCHA verification failed",
                mapOf("errors" to "reCAPTCHA score(${googleResponse.score}) is too low")
            )
        }
    }

    fun throwReCaptchaException(
        message: String,
        errors: Map<String, String> = mapOf(),
        throwable: Throwable? = null
    ): Nothing =
        throw throw ReCaptchaException(ErrorCode.REG_RECAPTCHA, message, throwable, errors)
}
