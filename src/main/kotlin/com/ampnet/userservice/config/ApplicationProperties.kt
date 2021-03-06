package com.ampnet.userservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.userservice")
class ApplicationProperties {
    val jwt: JwtProperties = JwtProperties()
    val mail: MailProperties = MailProperties()
    val user: UserProperties = UserProperties()
    val coop: CoopProperties = CoopProperties()
    val reCaptcha: ReCaptchaProperties = ReCaptchaProperties()
    val fileStorage: FileStorageProperties = FileStorageProperties()
    val veriff: VeriffProperties = VeriffProperties()
    val identyum: IdentyumProperties = IdentyumProperties()
}

@Suppress("MagicNumber")
class JwtProperties {
    lateinit var publicKey: String
    lateinit var privateKey: String
    var accessTokenValidityInMinutes: Long = 60 * 24
    var refreshTokenValidityInMinutes: Long = 60 * 24 * 90

    fun accessTokenValidityInMilliseconds(): Long = accessTokenValidityInMinutes * 60 * 1000
    fun refreshTokenValidityInMilliseconds(): Long = refreshTokenValidityInMinutes * 60 * 1000
}

class MailProperties {
    var confirmationNeeded = true
}

class UserProperties {
    var firstAdmin: Boolean = true
}

class CoopProperties {
    var default: String = "ampnet"
    var enableCreating: Boolean = true
}

@Suppress("MagicNumber")
class ReCaptchaProperties {
    var enabled: Boolean = false
    lateinit var secret: String
    var score: Float = 0.5F
    var url = "https://www.google.com/recaptcha/api/siteverify"
}

class FileStorageProperties {
    lateinit var url: String
    lateinit var bucket: String
    lateinit var folder: String
}

class VeriffProperties {
    lateinit var apiKey: String
    lateinit var privateKey: String
    var baseUrl: String = "https://stationapi.veriff.com"
}

class IdentyumProperties {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
    lateinit var publicKey: String
    lateinit var ampnetPrivateKey: String
    lateinit var webComponentUrl: String
    var instance: String = "default"
}
