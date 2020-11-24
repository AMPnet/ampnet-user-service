package com.ampnet.userservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "com.ampnet.userservice")
class ApplicationProperties {
    val jwt: JwtProperties = JwtProperties()
    val mail: MailProperties = MailProperties()
    val identyum: IdentyumProperties = IdentyumProperties()
    val user: UserProperties = UserProperties()
    val grpc: GrpcProperties = GrpcProperties()
    val coop: CoopProperties = CoopProperties()
    val reCaptcha: ReCaptchaProperties = ReCaptchaProperties()
}

@Suppress("MagicNumber")
class JwtProperties {
    lateinit var publicKey: String
    lateinit var privateKey: String
    var coopId = "ampnet"
    var accessTokenValidityInMinutes: Long = 60 * 24
    var refreshTokenValidityInMinutes: Long = 60 * 24 * 90

    fun accessTokenValidityInMilliseconds(): Long = accessTokenValidityInMinutes * 60 * 1000
    fun refreshTokenValidityInMilliseconds(): Long = refreshTokenValidityInMinutes * 60 * 1000
}

class MailProperties {
    var confirmationNeeded = true
}

class IdentyumProperties {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
    lateinit var publicKey: String
    lateinit var ampnetPrivateKey: String
}

class UserProperties {
    var firstAdmin: Boolean = true
}

@Suppress("MagicNumber")
class GrpcProperties {
    var mailServiceTimeout: Long = 2000
}

class CoopProperties {
    var default: String = "ampnet"
}

@Suppress("MagicNumber")
class ReCaptchaProperties {
    var enabled: Boolean = false
    lateinit var secret: String
    var score: Float = 0.9F
    lateinit var url: String
}
