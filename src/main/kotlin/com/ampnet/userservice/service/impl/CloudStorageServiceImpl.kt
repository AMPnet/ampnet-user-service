package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InternalException
import com.ampnet.userservice.service.CloudStorageService
import mu.KLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.net.URI
import java.time.ZonedDateTime

@Service
class CloudStorageServiceImpl(applicationProperties: ApplicationProperties) : CloudStorageService {

    companion object : KLogging()

    private val endpoint = applicationProperties.fileStorage.url
    private val bucketName = applicationProperties.fileStorage.bucket
    private val folder = applicationProperties.fileStorage.folder
    private val acl = ObjectCannedACL.PUBLIC_READ
    private val multipleSpacesPattern = "\\s+".toRegex()

    // Credentials are provided via Env variables: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
    private val s3client: S3Client by lazy {
        S3Client.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .region(Region.EU_CENTRAL_1)
            .endpointOverride(URI(endpoint))
            .build()
    }

    @Throws(InternalException::class)
    override fun saveFile(name: String, content: ByteArray): String {
        val key = getKeyFromName(name)
        try {
            s3client.putObject(
                PutObjectRequest.builder().acl(acl).bucket(bucketName).key("$folder/$key").build(),
                RequestBody.fromBytes(content)
            )
            logger.info { "Saved file: $key" }
            return getFileLink(key)
        } catch (ex: S3Exception) {
            logger.warn { ex.message }
            throw InternalException(
                ErrorCode.INT_FILE_STORAGE,
                "Could not store file with key: $key on cloud\n" +
                    "Exception message: ${ex.message}"
            )
        }
    }

    // Only for testing
    fun printObjectsFromBucket() {
        val list = s3client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
        println("List name = " + list.name())
        println("List size = " + list.contents().size)
        list.contents().forEach { println(it.key()) }
    }

    fun getFileLink(key: String): String {
        val delimiter = "//"
        val splittedEndpoint = endpoint.split(delimiter)
        return splittedEndpoint[0] + delimiter + bucketName + "." + splittedEndpoint[1] + "/" + folder + "/" + key
    }

    fun getKeyFromName(name: String): String {
        val timestamp = ZonedDateTime.now().toEpochSecond()
        val nameWithoutSpaces = name.replace(multipleSpacesPattern, "_")
        return nameWithoutSpaces.replaceFirst(".", "-$timestamp.")
    }
}
