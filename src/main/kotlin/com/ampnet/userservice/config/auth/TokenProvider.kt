package com.ampnet.userservice.config.auth

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.TokenException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.readValue
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.JacksonDeserializer
import io.jsonwebtoken.io.JacksonSerializer
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.Date
import javax.crypto.SecretKey

@Component
class TokenProvider(val applicationProperties: ApplicationProperties, val objectMapper: ObjectMapper) : Serializable {

    private val userKey = "user"
    private val key: SecretKey = Keys.hmacShaKeyFor(applicationProperties.jwt.signingKey.toByteArray())

    fun generateToken(userPrincipal: UserPrincipal): String {
        return Jwts.builder()
                .serializeToJsonWith(JacksonSerializer(objectMapper))
                .setSubject(userPrincipal.email)
                .claim(userKey, objectMapper.writeValueAsString(userPrincipal))
                .signWith(key, SignatureAlgorithm.HS256)
                .setIssuedAt(Date())
                .setExpiration(Date(System.currentTimeMillis() + applicationProperties.jwt.accessTokenValidity))
                .compact()
    }

    @Throws(TokenException::class)
    fun parseToken(token: String): UserPrincipal {
        try {
            val jwtParser = Jwts.parser()
                .deserializeJsonWith(JacksonDeserializer(objectMapper))
                .setSigningKey(key)
            val claimsJws = jwtParser.parseClaimsJws(token)
            val claims = claimsJws.body
            validateExpiration(claims)
            return getUserPrincipal(claims)
        } catch (ex: JwtException) {
            throw TokenException("Could not validate JWT token", ex)
        }
    }

    private fun validateExpiration(claims: Claims) {
        val expiration = claims.expiration
        if (Date().after(expiration)) {
            throw TokenException("Token expired. Expiration: $expiration")
        }
    }

    private fun getUserPrincipal(claims: Claims): UserPrincipal {
        val principalClaims = claims[userKey] as? String
                ?: throw TokenException("Token principal claims in invalid format")
        try {
            return objectMapper.readValue(principalClaims)
        } catch (ex: MissingKotlinParameterException) {
            throw TokenException("Could not extract user principal from JWT token for key: $userKey", ex)
        }
    }
}
