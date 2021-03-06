package com.ampnet.userservice.controller

import com.ampnet.userservice.controller.pojo.request.BankAccountRequest
import com.ampnet.userservice.controller.pojo.response.BankAccountListResponse
import com.ampnet.userservice.controller.pojo.response.BankAccountResponse
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.persistence.model.BankAccount
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.persistence.repository.BankAccountRepository
import com.ampnet.userservice.security.WithMockCrowdfundUser
import com.fasterxml.jackson.module.kotlin.readValue
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
import java.time.ZonedDateTime
import java.util.UUID

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
    @WithMockCrowdfundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustBeAbleToGetBankAccounts() {
        suppose("User has multiple bank accounts") {
            val firstAccount = createBankAccount(testContext.iban, testContext.bic)
            val secondAccount = createBankAccount("AZ96AZEJ00000000001234567890", "NTSBDEB1")
            testContext.bankAccounts = listOf(firstAccount, secondAccount)
        }

        verify("User can get a list of bank accounts") {
            val result = mockMvc.perform(get(bankAccountPath))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val bankAccounts: BankAccountListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(bankAccounts.bankAccounts).hasSize(2)
            assertThat(bankAccounts.bankAccounts.map { it.iban }).containsAll(testContext.bankAccounts.map { it.iban })
            assertThat(bankAccounts.bankAccounts.map { it.bankCode })
                .containsAll(testContext.bankAccounts.map { it.bankCode })
        }
    }

    @Test
    @WithMockCrowdfundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustBeAbleToCreateBankAccount() {
        verify("User can create IBAN bank account") {
            val request = BankAccountRequest(
                testContext.iban, testContext.bic, testContext.alias,
                testContext.bankName, testContext.bankAddress, testContext.beneficiaryName,
                testContext.beneficiaryAddress, testContext.beneficiaryCity, testContext.beneficiaryCountry
            )
            val result = mockMvc.perform(
                post(bankAccountPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()

            val bankAccount: BankAccountResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(bankAccount.iban).isEqualTo(testContext.iban)
            assertThat(bankAccount.bankCode).isEqualTo(testContext.bic)
            assertThat(bankAccount.alias).isEqualTo(testContext.alias)
            assertThat(bankAccount.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(bankAccount.id).isNotNull
            assertThat(bankAccount.bankName).isEqualTo(testContext.bankName)
            assertThat(bankAccount.bankAddress).isEqualTo(testContext.bankAddress)
            assertThat(bankAccount.beneficiaryName).isEqualTo(testContext.beneficiaryName)
            assertThat(bankAccount.beneficiaryAddress).isEqualTo(testContext.beneficiaryAddress)
            assertThat(bankAccount.beneficiaryCity).isEqualTo(testContext.beneficiaryCity)
            assertThat(bankAccount.beneficiaryCountry).isEqualTo(testContext.beneficiaryCountry)
        }
        verify("Bank account is stored") {
            val accounts = bankAccountRepository.findByUserUuid(user.uuid)
            assertThat(accounts).hasSize(1)
            val bankAccount = accounts.first()
            assertThat(bankAccount.iban).isEqualTo(testContext.iban)
            assertThat(bankAccount.bankCode).isEqualTo(testContext.bic)
            assertThat(bankAccount.alias).isEqualTo(testContext.alias)
            assertThat(bankAccount.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(bankAccount.id).isNotNull
            assertThat(bankAccount.bankName).isEqualTo(testContext.bankName)
            assertThat(bankAccount.bankAddress).isEqualTo(testContext.bankAddress)
            assertThat(bankAccount.beneficiaryName).isEqualTo(testContext.beneficiaryName)
            assertThat(bankAccount.beneficiaryAddress).isEqualTo(testContext.beneficiaryAddress)
            assertThat(bankAccount.beneficiaryCity).isEqualTo(testContext.beneficiaryCity)
            assertThat(bankAccount.beneficiaryCountry).isEqualTo(testContext.beneficiaryCountry)
        }
    }

    @Test
    @WithMockCrowdfundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
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
    @WithMockCrowdfundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustReturnBadRequestForInvalidIban() {
        verify("User cannot create invalid bank account") {
            val request = BankAccountRequest(
                "invalid-iban", testContext.bic,
                null, null, null, null, null, null, null
            )
            mockMvc.perform(
                post(bankAccountPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    @WithMockCrowdfundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustReturnBadRequestForInvalidBankCode() {
        verify("User cannot create invalid bank account") {
            val request = BankAccountRequest(
                testContext.iban, "invalid-bank-code",
                null, null, null, null, null, null, null
            )
            mockMvc.perform(
                post(bankAccountPath)
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Test
    @WithMockCrowdfundUser(uuid = "8a733721-9bb3-48b1-90b9-6463ac1493eb")
    fun mustThrowExceptionForTooLongBankAccountAlias() {
        verify("Admin can create bank account") {
            val request = BankAccountRequest(
                testContext.iban, testContext.bic, "aaa".repeat(50),
                null, null, null, null, null, null
            )
            val result = mockMvc.perform(
                post(bankAccountPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(result, ErrorCode.INT_REQUEST)
        }
    }

    private fun createBankAccount(
        account: String,
        format: String = "IBAN",
        alias: String = "alias",
        bankName: String = "bank name",
        bankAddress: String = "bank address",
        beneficiaryName: String = "beneficiary"
    ): BankAccount {
        val request = BankAccountRequest(
            account, format, alias, bankName, bankAddress, beneficiaryName,
            testContext.beneficiaryAddress, testContext.beneficiaryCity, testContext.beneficiaryCountry
        )
        val bankAccount = BankAccount(user, request)
        return bankAccountRepository.save(bankAccount)
    }

    private class TestContext {
        lateinit var bankAccounts: List<BankAccount>
        val iban = "HR1723600001101234565"
        val bic = "DABAIE2D"
        val alias = "alias"
        val bankName = "XYZ bank"
        val bankAddress = "XYZ address"
        val beneficiaryName = "Ampnet coop"
        val beneficiaryAddress = "Beneficiary address"
        val beneficiaryCity = "Beneficiary city"
        val beneficiaryCountry = "Beneficiary country"
    }
}
