package com.ampnet.userservice.service.pojo

import com.ampnet.userservice.enums.KycProvider
import com.ampnet.userservice.persistence.model.Coop
import com.fasterxml.jackson.annotation.JsonRawValue
import java.time.ZonedDateTime

data class CoopServiceResponse(
    val identifier: String,
    val name: String,
    val createdAt: ZonedDateTime,
    val hostname: String?,
    @field:JsonRawValue val config: String?,
    val logo: String?,
    val banner: String?,
    val needUserVerification: Boolean,
    val kycProvider: KycProvider
) {
    constructor(coop: Coop) : this(
        coop.identifier,
        coop.name,
        coop.createdAt,
        coop.hostname,
        coop.config,
        coop.logo,
        coop.banner,
        coop.needUserVerification,
        coop.kycProvider
    )
}
