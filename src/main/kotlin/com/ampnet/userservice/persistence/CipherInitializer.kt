package com.ampnet.userservice.persistence

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CipherInitializer {

    @Throws(
        InvalidKeyException::class,
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class
    )
    fun prepareAndInitCipher(encryptionMode: Int, key: String): Cipher {
        val cipher = Cipher.getInstance(CIPHER_INSTANCE_NAME)
        val secretKey: Key = SecretKeySpec(key.toByteArray(), SECRET_KEY_ALGORITHM)
        val algorithmParameters = getAlgorithmParameterSpec(cipher)
        cipher.init(encryptionMode, secretKey, algorithmParameters)
        return cipher
    }

    private fun getAlgorithmParameterSpec(cipher: Cipher): AlgorithmParameterSpec =
        IvParameterSpec(ByteArray(cipher.blockSize))
}

const val CIPHER_INSTANCE_NAME = "AES/CBC/PKCS5Padding"
const val SECRET_KEY_ALGORITHM = "AES"
