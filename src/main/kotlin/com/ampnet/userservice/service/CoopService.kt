package com.ampnet.userservice.service

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.service.pojo.CoopServiceResponse
import org.springframework.web.multipart.MultipartFile

interface CoopService {
    fun createCoop(request: CoopRequest, logo: MultipartFile?, banner: MultipartFile?): CoopServiceResponse
    fun getCoopByIdentifier(identifier: String): CoopServiceResponse?
    fun getCoopByHostname(host: String): CoopServiceResponse?
    fun updateCoop(
        identifier: String,
        request: CoopUpdateRequest,
        logo: MultipartFile?,
        banner: MultipartFile?
    ): CoopServiceResponse?
}
