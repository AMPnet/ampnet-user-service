package com.ampnet.userservice.controller.pojo.response

import com.ampnet.userservice.persistence.model.BankAccount
import java.time.ZonedDateTime

data class BankAccountResponse(
    val id: Int,
    val iban: String,
    val bankCode: String,
    val createdAt: ZonedDateTime,
    val alias: String?,
    val bankName: String?,
    val bankAddress: String?,
    val beneficiaryName: String?,
    val beneficiaryAddress: String?,
    val beneficiaryCity: String?,
    val beneficiaryCountry: String?,
) {
    constructor(bankAccount: BankAccount) : this(
        bankAccount.id,
        bankAccount.iban,
        bankAccount.bankCode,
        bankAccount.createdAt,
        bankAccount.alias,
        bankAccount.bankName,
        bankAccount.bankAddress,
        bankAccount.beneficiaryName,
        bankAccount.beneficiaryAddress,
        bankAccount.beneficiaryCity,
        bankAccount.beneficiaryCountry
    )
}

data class BankAccountListResponse(val bankAccounts: List<BankAccountResponse>)
