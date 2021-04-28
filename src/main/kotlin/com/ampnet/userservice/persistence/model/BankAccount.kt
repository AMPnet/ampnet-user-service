package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.controller.pojo.request.BankAccountRequest
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
@Suppress("LongParameterList")
class BankAccount(
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
    val alias: String?,

    @Column(nullable = true, length = 128)
    val bankName: String?,

    @Column(nullable = true, length = 128)
    val bankAddress: String?,

    @Column(nullable = true, length = 128)
    val beneficiaryName: String?,

    @Column(nullable = true, length = 256)
    val beneficiaryAddress: String?,

    @Column(nullable = true, length = 64)
    val beneficiaryCity: String?,

    @Column(nullable = true, length = 64)
    val beneficiaryCountry: String?
) {
    constructor(
        user: User,
        request: BankAccountRequest
    ) : this(
        0, user, request.iban, request.bankCode, ZonedDateTime.now(),
        request.alias, request.bankName, request.bankAddress, request.beneficiaryName,
        request.beneficiaryAddress, request.beneficiaryCity, request.beneficiaryCountry
    )
}
