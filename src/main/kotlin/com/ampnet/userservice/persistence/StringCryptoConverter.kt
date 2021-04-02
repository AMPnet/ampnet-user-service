package com.ampnet.userservice.persistence

import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InternalException
import org.springframework.beans.factory.annotation.Value
import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class StringCryptoConverter : AttributeConverter<String, String> {

    @Value("\${spring.flyway.placeholders.private_key}")
    private lateinit var databaseEncryptionKeyInBase64: String

    @Throws(InternalException::class)
    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute.isNullOrBlank()) return attribute
        try {
            val cipher = createCipher(Cipher.ENCRYPT_MODE)
            val bytesToEncrypt = attribute.toByteArray()
            val encryptedBytes = cipher.doFinal(bytesToEncrypt)
            return Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (ex: GeneralSecurityException) {
            throw InternalException(ErrorCode.INT_ENCRYPTION, ex.localizedMessage)
        }
    }

    @Throws(InternalException::class)
    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData.isNullOrBlank()) return dbData
        try {
            val cipher = createCipher(Cipher.DECRYPT_MODE)
            val encryptedBytes: ByteArray = Base64.getDecoder().decode(dbData.replace("\n", ""))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes)
        } catch (ex: GeneralSecurityException) {
            throw InternalException(ErrorCode.INT_ENCRYPTION, ex.localizedMessage)
        }
    }

    @Throws(
        InvalidAlgorithmParameterException::class, InvalidKeyException::class,
        NoSuchAlgorithmException::class, NoSuchPaddingException::class
    )
    private fun createCipher(encryptionMode: Int): Cipher =
        Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            val decodedKey = Base64.getDecoder().decode(databaseEncryptionKeyInBase64)
            val secretKey: Key = SecretKeySpec(decodedKey, "AES")
            val algorithmParameters = IvParameterSpec(ByteArray(this.blockSize))
            init(encryptionMode, secretKey, algorithmParameters)
        }
}
