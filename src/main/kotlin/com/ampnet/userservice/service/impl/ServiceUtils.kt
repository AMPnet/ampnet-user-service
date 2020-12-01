package com.ampnet.userservice.service.impl

import org.springframework.web.multipart.MultipartFile
import java.util.Optional

internal object ServiceUtils {
    fun <T> wrapOptional(optional: Optional<T>): T? {
        return if (optional.isPresent) optional.get() else null
    }

    fun getImageNameFromMultipartFile(multipartFile: MultipartFile): String =
        multipartFile.originalFilename ?: multipartFile.name
}
