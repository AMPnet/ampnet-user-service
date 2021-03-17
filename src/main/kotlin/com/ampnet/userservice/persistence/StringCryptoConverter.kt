package com.ampnet.userservice.persistence

import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InternalException
import org.springframework.beans.factory.annotation.Value
import java.security.GeneralSecurityException
import java.util.Base64
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class StringCryptoConverter : AttributeConverter<String, String> {

    private val cipherInitializer = CipherInitializer()

    @Value("\${spring.flyway.placeholders.private_key}")
    private lateinit var databaseEncryptionKey: String

    @Throws(InternalException::class)
    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute.isNullOrBlank()) return attribute
        try {
            val cipher = cipherInitializer.prepareAndInitCipher(Cipher.ENCRYPT_MODE, databaseEncryptionKey)
            return encrypt(cipher, attribute)
        } catch (ex: GeneralSecurityException) {
            throw InternalException(ErrorCode.INT_ENCRYPTION, ex.localizedMessage)
        }
    }

    @Throws(InternalException::class)
    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData.isNullOrBlank()) return dbData
        try {
            val cipher = cipherInitializer.prepareAndInitCipher(Cipher.DECRYPT_MODE, databaseEncryptionKey)
            return decrypt(cipher, dbData)
        } catch (ex: GeneralSecurityException) {
            throw InternalException(ErrorCode.INT_ENCRYPTION, ex.localizedMessage)
        }
    }

    @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
    private fun encrypt(cipher: Cipher, attribute: String): String {
        val bytesToEncrypt = attribute.toByteArray()
        val encryptedBytes = cipher.doFinal(bytesToEncrypt)
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
    private fun decrypt(cipher: Cipher, dbData: String): String {
        val encryptedBytes: ByteArray = Base64.getDecoder().decode(dbData)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }
}
