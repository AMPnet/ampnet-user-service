package com.ampnet.userservice.config

import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerService(val em: EntityManager) {

    @Transactional
    fun deleteAllUsers() {
        em.createNativeQuery("TRUNCATE app_user CASCADE").executeUpdate()
        deleteAllUserInfos()
    }

    @Transactional
    fun deleteAllUserInfos() {
        em.createNativeQuery("TRUNCATE user_info CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllMailTokens() {
        em.createNativeQuery("TRUNCATE mail_token CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllRefreshTokens() {
        em.createNativeQuery("TRUNCATE refresh_token CASCADE").executeUpdate()
    }
}
