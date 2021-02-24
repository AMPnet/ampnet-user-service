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
        callCipherInit(cipher, encryptionMode, secretKey, algorithmParameters)
        return cipher
    }

    @Throws(InvalidKeyException::class, InvalidAlgorithmParameterException::class)
    fun callCipherInit(
        cipher: Cipher,
        encryptionMode: Int,
        secretKey: Key,
        algorithmParameters: AlgorithmParameterSpec
    ) =
        cipher.init(encryptionMode, secretKey, algorithmParameters)

    private fun getCipherBlockSize(cipher: Cipher): Int = cipher.blockSize

    private fun getAlgorithmParameterSpec(cipher: Cipher): AlgorithmParameterSpec =
        IvParameterSpec(ByteArray(getCipherBlockSize(cipher)))
}

const val CIPHER_INSTANCE_NAME = "AES/CBC/PKCS5Padding"
const val SECRET_KEY_ALGORITHM = "AES"
