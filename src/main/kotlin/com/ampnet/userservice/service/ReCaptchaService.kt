package com.ampnet.userservice.service

interface ReCaptchaService {
    fun validateResponseToken(reCaptchaToken: String)
}
