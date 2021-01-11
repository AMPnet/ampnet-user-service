package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.enums.KycProvider
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "coop")
class Coop(

    @Id
    val identifier: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(nullable = false)
    var needUserVerification: Boolean,

    @Column
    var hostname: String?,

    @Column
    var config: String?,

    @Column
    var logo: String?,

    @Column(name = "kyc_provider_id")
    var kycProvider: KycProvider

) {
    constructor(
        identifier: String,
        name: String,
        hostname: String?,
        config: String?,
        logo: String?,
        kycProvider: KycProvider = KycProvider.IDENTYUM
    ) : this(
        identifier,
        name,
        ZonedDateTime.now(),
        true,
        hostname,
        config,
        logo,
        kycProvider
    )
}
