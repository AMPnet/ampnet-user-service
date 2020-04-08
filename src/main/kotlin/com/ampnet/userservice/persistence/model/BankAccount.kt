package com.ampnet.userservice.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "bank_account")
data class BankAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @ManyToOne
    @JoinColumn(name = "user_uuid")
    val user: User,

    @Column(nullable = false, length = 64)
    val iban: String,

    @Column(nullable = false, length = 16)
    val bankCode: String,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(length = 128)
    val alias: String?
) {
    constructor(user: User, iban: String, bankCode: String, alias: String?) : this(
        0, user, iban, bankCode, ZonedDateTime.now(), alias
    )
}
