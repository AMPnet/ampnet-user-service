package com.ampnet.userservice.service

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.service.pojo.CoopServiceResponse

interface CoopService {
    fun createCoop(request: CoopRequest): CoopServiceResponse
    fun getCoopByIdentifier(identifier: String): CoopServiceResponse?
    fun getCoopByHostname(host: String): CoopServiceResponse?
    fun updateCoop(identifier: String, request: CoopUpdateRequest): CoopServiceResponse?
}
