package com.ampnet.userservice.service

interface ReCaptchaService {
    fun processResponseToken(reCaptchaToken: String, remoteIp: String)
}
