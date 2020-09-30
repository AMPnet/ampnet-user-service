package com.ampnet.userservice.controller.pojo.response

import com.ampnet.userservice.persistence.model.Coop
import java.time.ZonedDateTime

data class CoopResponse(
    val id: Int,
    val name: String,
    val identifier: String,
    val createdAt: ZonedDateTime
) {
    constructor(coop: Coop) : this(coop.id, coop.name, coop.identifier, coop.createdAt)
}
