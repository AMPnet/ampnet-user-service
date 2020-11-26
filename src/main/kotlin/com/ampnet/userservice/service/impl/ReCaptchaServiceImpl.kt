package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.ReCaptchaException
import com.ampnet.userservice.service.ReCaptchaService
import com.ampnet.userservice.service.pojo.GoogleResponse
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class ReCaptchaServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) : ReCaptchaService {

    @Throws(ReCaptchaException::class)
    override fun validateResponseToken(reCaptchaToken: String) {
        if (applicationProperties.reCaptcha.enabled.not()) return
        try {
            val responseEntity = restTemplate.postForEntity<String>(generateGoogleUri(reCaptchaToken))
            val googleResponse = readGoogleResponse(responseEntity)
            if (responseEntity.statusCode.is2xxSuccessful) {
                if (googleResponse.success) {
                    validateScore(googleResponse)
                    return
                }
            }
            val joinedErrors = googleResponse.errorCodes.joinToString()
            throw ReCaptchaException("errors: $joinedErrors")
        } catch (ex: RestClientException) {
            throw ReCaptchaException("Invalid response from Google's server", ex)
        }
    }

    private fun validateScore(googleResponse: GoogleResponse) {
        if (googleResponse.score < applicationProperties.reCaptcha.score) {
            throw ReCaptchaException(
                "ReCAPTCHA score(${googleResponse.score}) is too low"
            )
        }
    }

    private fun readGoogleResponse(responseEntity: ResponseEntity<String>): GoogleResponse {
        val googleResponse = responseEntity.body
            ?: throw ReCaptchaException("Empty response from Google's server")
        try {
            return objectMapper.readValue(googleResponse, GoogleResponse::class.java)
        } catch (ex: JsonProcessingException) {
            throw ReCaptchaException("Error while reading google response", ex)
        }
    }

    private fun generateGoogleUri(reCaptchaToken: String): URI {
        return UriComponentsBuilder
            .fromUriString(applicationProperties.reCaptcha.url)
            .queryParam("secret", applicationProperties.reCaptcha.secret)
            .queryParam("response", reCaptchaToken)
            .build()
            .toUri()
    }
}
