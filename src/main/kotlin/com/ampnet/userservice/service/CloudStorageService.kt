package com.ampnet.userservice.service

interface CloudStorageService {
    fun saveLogo(name: String, content: ByteArray): String
}
