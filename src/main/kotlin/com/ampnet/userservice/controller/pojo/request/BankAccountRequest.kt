package com.ampnet.userservice.controller.pojo.request

data class BankAccountRequest(
    val iban: String,
    val bankCode: String,
    val alias: String?
)
