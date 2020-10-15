package com.ampnet.userservice.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "forgot_password_token")
class ForgotPasswordToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @ManyToOne
    @JoinColumn(name = "user_uuid")
    val user: User,

    @Column(nullable = false)
    val token: UUID,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    fun isExpired(): Boolean {
        return createdAt.plusHours(1).isBefore(ZonedDateTime.now())
    }
}
