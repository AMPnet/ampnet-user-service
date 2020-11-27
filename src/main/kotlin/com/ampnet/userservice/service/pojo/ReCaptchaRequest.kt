package com.ampnet.userservice.service.pojo

data class ReCaptchaRequest(
    val secret: String,
    val token: String
)
