package com.ampnet.userservice.service

interface ReCaptchaService {
    fun processUserResponse(reCaptchaToken: String, remoteIp: String)
}