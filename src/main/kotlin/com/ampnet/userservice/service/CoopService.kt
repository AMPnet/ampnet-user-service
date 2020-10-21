package com.ampnet.userservice.service

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.persistence.model.Coop

interface CoopService {
    fun createCoop(request: CoopRequest): Coop
    fun getCoopByIdentifier(identifier: String): Coop?
    fun getCoopByHost(host: String): Coop?
    fun updateCoop(identifier: String, request: CoopUpdateRequest): Coop?
}
