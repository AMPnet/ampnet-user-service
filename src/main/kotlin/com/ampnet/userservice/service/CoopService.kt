package com.ampnet.userservice.service

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.persistence.model.Coop

interface CoopService {
    fun createCoop(request: CoopRequest): Coop
    fun getCoopByIdentifier(identifier: String): Coop?
}
