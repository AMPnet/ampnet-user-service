package com.ampnet.userservice.service

interface CloudStorageService {
    fun saveFile(name: String, content: ByteArray): String
}
