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
}

@Suppress("MagicNumber")
class JwtProperties {
    lateinit var signingKey: String
    var accessTokenValidity: Long = 60 * 60 * 24 * 60
    var refreshTokenValidity: Long = 60 * 60 * 24 * 356
}

class MailProperties {
    var confirmationNeeded = true
}

class IdentyumProperties {
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String
    lateinit var key: String
}

class UserProperties {
    var creatingTestUser: Boolean = false
    var firstAdmin: Boolean = true
}

@Suppress("MagicNumber")
class GrpcProperties {
    var mailServiceTimeout: Long = 2000
}
