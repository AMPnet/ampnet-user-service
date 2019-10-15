package com.ampnet.userservice.service.impl

import com.ampnet.core.jwt.JwtTokenUtils
import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.core.jwt.exception.TokenException
import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.persistence.model.RefreshToken
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.RefreshTokenRepository
import com.ampnet.userservice.service.TokenService
import com.ampnet.userservice.service.pojo.AccessAndRefreshToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class TokenServiceImpl(
    private val applicationProperties: ApplicationProperties,
    private val refreshTokenRepository: RefreshTokenRepository
) : TokenService {

    private companion object {
        const val REFRESH_TOKEN_LENGTH = 128
    }
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('-', '_', '+')

    @Transactional
    override fun generateAccessAndRefreshForUser(user: User): AccessAndRefreshToken {
        val token = getRandomToken()
        val refreshToken = refreshTokenRepository.save(RefreshToken(0, user, token, ZonedDateTime.now()))
        val accessToken = JwtTokenUtils.encodeToken(
            generateUserPrincipalFromUser(user),
            applicationProperties.jwt.signingKey,
            applicationProperties.jwt.accessTokenValidity
        )
        return AccessAndRefreshToken(
            accessToken,
            applicationProperties.jwt.accessTokenValidity,
            refreshToken.token,
            applicationProperties.jwt.refreshTokenValidity
        )
    }

    @Throws(TokenException::class)
    @Transactional
    override fun generateAccessAndRefreshFromRefreshToken(token: String): AccessAndRefreshToken {
        val refreshToken = ServiceUtils.wrapOptional(refreshTokenRepository.findByToken(token))
            ?: throw TokenException("Non existing refresh token")
        val expiration = refreshToken.createdAt.plusSeconds(applicationProperties.jwt.refreshTokenValidity)
        val refreshTokenExpiresIn: Long = expiration.toEpochSecond() - ZonedDateTime.now().toEpochSecond()
        if (refreshTokenExpiresIn <= 0) {
            refreshTokenRepository.delete(refreshToken)
            throw TokenException("Refresh token expired")
        }
        val accessToken = JwtTokenUtils.encodeToken(
            generateUserPrincipalFromUser(refreshToken.user),
            applicationProperties.jwt.signingKey,
            applicationProperties.jwt.accessTokenValidity
        )
        return AccessAndRefreshToken(
            accessToken,
            applicationProperties.jwt.accessTokenValidity,
            refreshToken.token,
            refreshTokenExpiresIn
        )
    }

    @Transactional
    override fun deleteRefreshToken(userUuid: UUID) {
        ServiceUtils.wrapOptional(refreshTokenRepository.findByUserUuid(userUuid))?.let {
            refreshTokenRepository.delete(it)
        }
    }

    private fun getRandomToken(): String = (1..REFRESH_TOKEN_LENGTH)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    private fun generateUserPrincipalFromUser(user: User) = UserPrincipal(
        user.uuid,
        user.email,
        user.getFullName(),
        user.getAuthorities().asSequence().map { it.authority }.toSet(),
        user.enabled
    )
}
