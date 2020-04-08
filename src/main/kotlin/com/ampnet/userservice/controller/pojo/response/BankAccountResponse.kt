package com.ampnet.userservice.controller.pojo.response

import com.ampnet.userservice.persistence.model.BankAccount
import java.time.ZonedDateTime

data class BankAccountResponse(
    val id: Int,
    val iban: String,
    val bankCode: String,
    val createdAt: ZonedDateTime,
    val alias: String?
) {
    constructor(bankAccount: BankAccount) : this(
        bankAccount.id,
        bankAccount.iban,
        bankAccount.bankCode,
        bankAccount.createdAt,
        bankAccount.alias
    )
}

data class BankAccountListResponse(val bankAccounts: List<BankAccountResponse>)
