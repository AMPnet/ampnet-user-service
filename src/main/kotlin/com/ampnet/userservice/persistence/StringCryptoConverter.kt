package com.ampnet.userservice.persistence

import com.ampnet.userservice.exception.EncryptionException
import com.ampnet.userservice.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.Base64
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class StringCryptoConverter : AttributeConverter<String, String> {

    private val cipherInitializer = CipherInitializer()

    @Value("\${spring.flyway.placeholders.private_key}")
    private lateinit var databaseEncryptionKey: String

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        BadPaddingException::class,
        NoSuchPaddingException::class,
        IllegalBlockSizeException::class
    )
    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute.isNullOrBlank()) return attribute
        return try {
            val cipher = cipherInitializer.prepareAndInitCipher(Cipher.ENCRYPT_MODE, databaseEncryptionKey)
            encrypt(cipher, attribute)
        } catch (ex: GeneralSecurityException) {
            throw EncryptionException(ErrorCode.INT_ENCRYPTION, ex.localizedMessage)
        }
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        BadPaddingException::class,
        NoSuchPaddingException::class,
        IllegalBlockSizeException::class
    )
    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData.isNullOrBlank()) return dbData
        return try {
            val cipher = cipherInitializer.prepareAndInitCipher(Cipher.DECRYPT_MODE, databaseEncryptionKey)
            decrypt(cipher, dbData)
        } catch (ex: GeneralSecurityException) {
            throw EncryptionException(ErrorCode.INT_ENCRYPTION, ex.localizedMessage)
        }
    }

    private fun callCipherDoFinal(cipher: Cipher, bytes: ByteArray): ByteArray = cipher.doFinal(bytes)

    @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
    private fun encrypt(cipher: Cipher, attribute: String): String {
        val bytesToEncrypt = attribute.toByteArray()
        val encryptedBytes = callCipherDoFinal(cipher, bytesToEncrypt)
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
    private fun decrypt(cipher: Cipher, dbData: String): String {
        val encryptedBytes: ByteArray = Base64.getDecoder().decode(dbData)
        val decryptedBytes = callCipherDoFinal(cipher, encryptedBytes)
        return String(decryptedBytes)
    }
}
