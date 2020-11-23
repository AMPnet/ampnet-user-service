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

@Service
class ReCaptchaServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate
) : ReCaptchaService {

    override fun processUserResponse(reCaptchaToken: String, remoteIp: String) {
        if (!applicationProperties.reCaptcha.enabled) return
        val request = ReCaptchaRequest(
            applicationProperties.reCaptcha.secret,
            reCaptchaToken,
            remoteIp
        )
        try {
            val response = restTemplate.postForEntity(
                applicationProperties.reCaptcha.url, request, GoogleResponse::class.java)
            print(response)
//            if (response.statusCode.is2xxSuccessful) {
//                response.body?.let {
//                    if (it.success) return
//                }
//            }
//            val joinedErrors = response.body?.errorCodes?.joinToString() ?: "Failed to fetch errors"
//            throw ReCaptchaException(
//                ErrorCode.REG_RECAPTCHA,
//                "ReCAPTCHA verification failed", errors = mapOf("errors" to joinedErrors)
//            )
        } catch (ex: RestClientException) {
            throw ReCaptchaException(ErrorCode.REG_RECAPTCHA, "ReCAPTCHA verification failed", ex)
        }
    }
}