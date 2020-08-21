package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.BankAccountRequest
import com.ampnet.userservice.controller.pojo.response.BankAccountListResponse
import com.ampnet.userservice.controller.pojo.response.BankAccountResponse
import com.ampnet.userservice.service.BankAccountService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
class BankAccountController(
    private val bankAccountService: BankAccountService
) {

    companion object : KLogging()

    @GetMapping("/bank-account")
    fun getBankAccount(): ResponseEntity<BankAccountListResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to add bank account for user: ${userPrincipal.uuid}" }
        val accounts = bankAccountService.findBankAccounts(userPrincipal.uuid).map { BankAccountResponse(it) }
        val response = BankAccountListResponse(accounts)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/bank-account")
    fun createBankAccount(@RequestBody @Valid request: BankAccountRequest): ResponseEntity<BankAccountResponse> {
        logger.debug { "Received request to add bank account: $request" }
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        val bankAccount = bankAccountService.createBankAccount(userPrincipal.uuid, request)
        return ResponseEntity.ok(BankAccountResponse(bankAccount))
    }

    @DeleteMapping("/bank-account/{id}")
    fun deleteBankAccount(@PathVariable("id") id: Int): ResponseEntity<Unit> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to delete bank account for user: ${userPrincipal.uuid}" }
        bankAccountService.deleteBankAccount(userPrincipal.uuid, id)
        return ResponseEntity.ok().build()
    }
}
