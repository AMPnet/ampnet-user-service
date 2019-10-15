package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.BankAccountRequest
import com.ampnet.userservice.controller.pojo.response.BankAccountListResponse
import com.ampnet.userservice.controller.pojo.response.BankAccountResponse
import com.ampnet.userservice.persistence.model.BankAccount
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.BankAccountRepository
import com.ampnet.userservice.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.ZonedDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BankAccountControllerTest : ControllerTestBase() {

    @Autowired
    private lateinit var bankAccountRepository: BankAccountRepository

    private val bankAccountPath = "/bank-account"
    private val user: User by lazy {
        databaseCleanerService.deleteAllUsers()
        createUser(defaultEmail, uuid = UUID.fromString("8a733721-9bb3-48b1-90b9-6463ac1493eb"))
    }
    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        user.uuid
        databaseCleanerService.deleteAllBankAccounts()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustBeAbleToGetBankAccounts() {
        suppose("User has multiple bank accounts") {
            val firstAccount = createBankAccount(testContext.iban, testContext.bic)
            val secondAccount = createBankAccount("AZ96AZEJ00000000001234567890", "NTSBDEB1")
            testContext.bankAccounts = listOf(firstAccount, secondAccount)
        }

        verify("User can get a list of bank accounts") {
            val result = mockMvc.perform(get(bankAccountPath))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn()

            val bankAccounts: BankAccountListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(bankAccounts.bankAccounts).hasSize(2)
            assertThat(bankAccounts.bankAccounts.map { it.iban }).containsAll(testContext.bankAccounts.map { it.iban })
            assertThat(bankAccounts.bankAccounts.map { it.bankCode })
                .containsAll(testContext.bankAccounts.map { it.bankCode })
        }
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustBeAbleToCreateBankAccount() {
        verify("User can create IBAN bank account") {
            val request = BankAccountRequest(testContext.iban, testContext.bic)
            val result = mockMvc.perform(
                post(bankAccountPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn()

            val bankAccount: BankAccountResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(bankAccount.iban).isEqualTo(testContext.iban)
            assertThat(bankAccount.bankCode).isEqualTo(testContext.bic)
            assertThat(bankAccount.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(bankAccount.id).isNotNull()
        }
        verify("Bank account is stored") {
            val accounts = bankAccountRepository.findByUserUuid(user.uuid)
            assertThat(accounts).hasSize(1)
            val bankAccount = accounts.first()
            assertThat(bankAccount.iban).isEqualTo(testContext.iban)
            assertThat(bankAccount.bankCode).isEqualTo(testContext.bic)
            assertThat(bankAccount.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(bankAccount.id).isNotNull()
        }
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustBeAbleToDeleteAccount() {
        suppose("User has a bank accounts") {
            val ibanAccount = createBankAccount(testContext.iban)
            testContext.bankAccounts = listOf(ibanAccount)
        }

        verify("User can delete the bank account") {
            mockMvc.perform(delete("$bankAccountPath/${testContext.bankAccounts.first().id}"))
                .andExpect(status().isOk)
        }
        verify("Bank account is deleted") {
            val accounts = bankAccountRepository.findByUserUuid(user.uuid)
            assertThat(accounts).hasSize(0)
        }
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustReturnBadRequestForInvaliIban() {
        verify("User cannot create invalid bank account") {
            val request = BankAccountRequest("invalid-iban", testContext.bic)
            mockMvc.perform(
                post(bankAccountPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    @WithMockCrowdfoundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustReturnBadRequestForInvalidBankCode() {
        verify("User cannot create invalid bank account") {
            val request = BankAccountRequest(testContext.iban, "invalid-bank-code")
            mockMvc.perform(
                post(bankAccountPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest)
        }
    }

    private fun createBankAccount(account: String, format: String = "IBAN"): BankAccount {
        val bankAccount = BankAccount(0, user, account, format, ZonedDateTime.now())
        return bankAccountRepository.save(bankAccount)
    }

    private class TestContext {
        lateinit var bankAccounts: List<BankAccount>
        val iban = "HR1723600001101234565"
        val bic = "DABAIE2D"
    }
}
