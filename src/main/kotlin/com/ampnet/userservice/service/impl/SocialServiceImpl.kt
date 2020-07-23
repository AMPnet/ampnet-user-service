package com.ampnet.userservice.service.impl

import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.SocialException
import com.ampnet.userservice.service.SocialService
import com.ampnet.userservice.service.pojo.SocialUser
import mu.KLogging
import org.springframework.social.NotAuthorizedException
import org.springframework.social.facebook.api.User
import org.springframework.social.facebook.api.impl.FacebookTemplate
import org.springframework.social.google.api.impl.GoogleTemplate
import org.springframework.stereotype.Service

@Service
class SocialServiceImpl : SocialService {

    companion object : KLogging()

    @Throws(SocialException::class)
    override fun getFacebookEmail(token: String): SocialUser {
        logger.debug { "Getting Facebook user info" }
        try {
            val facebook = FacebookTemplate(token)
            val userProfile = facebook.fetchObject(
                "me",
                User::class.java,
                "id", "email", "firstName", "lastName"
            )
            logger.debug { "Received Facebook user info with mail: ${userProfile.email}" }
            return SocialUser(userProfile.email, userProfile.firstName, userProfile.lastName)
        } catch (ex: NotAuthorizedException) {
            throw SocialException(ErrorCode.REG_SOCIAL, "Not authorized to get data from Facebook", ex)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    @Throws(SocialException::class)
    override fun getGoogleEmail(token: String): SocialUser {
        logger.debug { "Getting Google user info" }
        try {
            val template = GoogleTemplate(token)
            val userInfo = template.oauth2Operations().userinfo
            logger.debug { "Received Google user info with mail: ${userInfo.email}" }
            return SocialUser(userInfo.email, userInfo.givenName, userInfo.familyName)
        } catch (ex: Exception) {
            throw SocialException(ErrorCode.REG_SOCIAL, "Cannot fetch data from Google", ex)
        }
    }
}
