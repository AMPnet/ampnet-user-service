package com.ampnet.userservice.service

import com.ampnet.userservice.persistence.model.Coop

interface CoopService {
    fun createCoop(name: String): Coop
    fun getCoopByIdentifier(identifier: String): Coop?
}
