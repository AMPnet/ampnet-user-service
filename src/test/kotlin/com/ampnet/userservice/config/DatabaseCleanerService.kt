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
        em.createNativeQuery("DELETE FROM mail_token").executeUpdate()
    }

    @Transactional
    fun deleteAllRefreshTokens() {
        em.createNativeQuery("DELETE FROM refresh_token").executeUpdate()
    }

    @Transactional
    fun deleteAllBankAccounts() {
        em.createNativeQuery("DELETE FROM bank_account").executeUpdate()
    }

    @Transactional
    fun deleteAllForgotPasswordTokens() {
        em.createNativeQuery("DELETE FROM forgot_password_token").executeUpdate()
    }
}
