package com.ampnet.userservice.service.impl

import com.ampnet.core.jwt.JwtTokenUtils
import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.core.jwt.exception.KeyException
import com.ampnet.core.jwt.exception.TokenException
import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.enums.UserRole
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ResourceNotFoundException
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.model.RefreshToken
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.CoopRepository
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
    private val refreshTokenRepository: RefreshTokenRepository,
    private val coopRepository: CoopRepository
) : TokenService {

    private companion object {
        const val REFRESH_TOKEN_LENGTH = 128
    }

    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('-', '_', '+')

    @Transactional
    @Throws(ResourceNotFoundException::class, KeyException::class, TokenException::class)
    override fun generateAccessAndRefreshForUser(user: User): AccessAndRefreshToken {
        deleteRefreshToken(user.uuid)
        val coop = getCoop(user.coop)
        val token = getRandomToken()
        val refreshToken = refreshTokenRepository.save(RefreshToken(0, user, token, ZonedDateTime.now()))
        val accessToken = JwtTokenUtils.encodeToken(
            generateUserPrincipalFromUser(user, coop.needUserVerification),
            applicationProperties.jwt.privateKey,
            applicationProperties.jwt.accessTokenValidityInMilliseconds()
        )
        return AccessAndRefreshToken(
            accessToken,
            applicationProperties.jwt.accessTokenValidityInMilliseconds(),
            refreshToken.token,
            applicationProperties.jwt.refreshTokenValidityInMilliseconds()
        )
    }

    @Transactional
    @Throws(TokenException::class, KeyException::class)
    override fun generateAccessAndRefreshFromRefreshToken(token: String): AccessAndRefreshToken {
        val refreshToken = ServiceUtils.wrapOptional(refreshTokenRepository.findByToken(token))
            ?: throw TokenException("Non existing refresh token")
        val expiration = refreshToken.createdAt
            .plusMinutes(applicationProperties.jwt.refreshTokenValidityInMinutes)
        val refreshTokenExpiresIn: Long = expiration.toEpochSecond() - ZonedDateTime.now().toEpochSecond()
        if (refreshTokenExpiresIn <= 0) {
            refreshTokenRepository.delete(refreshToken)
            throw TokenException("Refresh token expired")
        }
        val coop = getCoop(refreshToken.user.coop)
        val accessToken = JwtTokenUtils.encodeToken(
            generateUserPrincipalFromUser(refreshToken.user, coop.needUserVerification),
            applicationProperties.jwt.privateKey,
            applicationProperties.jwt.accessTokenValidityInMilliseconds()
        )
        return AccessAndRefreshToken(
            accessToken,
            applicationProperties.jwt.accessTokenValidityInMilliseconds(),
            refreshToken.token,
            refreshTokenExpiresIn
        )
    }

    @Transactional
    override fun deleteRefreshToken(userUuid: UUID) {
        refreshTokenRepository.deleteByUserUuid(userUuid)
    }

    private fun getCoop(coop: String): Coop =
        coopRepository.findByIdentifier(coop)
            ?: throw ResourceNotFoundException(ErrorCode.COOP_MISSING, "Missing coop: $coop")

    private fun getRandomToken(): String = (1..REFRESH_TOKEN_LENGTH)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

    private fun generateUserPrincipalFromUser(user: User, needVerification: Boolean): UserPrincipal {
        val verified = if (needVerification) {
            (user.userInfoUuid != null || user.role == UserRole.ADMIN)
        } else {
            true
        }
        return UserPrincipal(
            user.uuid,
            user.email,
            user.getFullName(),
            user.getAuthorities().asSequence().map { it.authority }.toSet(),
            user.enabled,
            verified,
            user.coop
        )
    }
}
