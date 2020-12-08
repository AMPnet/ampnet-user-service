package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.service.pojo.VeriffSessionResponse
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "veriff_session")
class VeriffSession(
    @Id
    val id: String,

    @Column(nullable = false)
    val userUuid: UUID,

    @Column
    val url: String,

    @Column
    val vendorData: String,

    @Column
    val host: String,

    @Column
    val status: String,

    @Column(nullable = false)
    var connected: Boolean,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    constructor(veriffSessionResponse: VeriffSessionResponse, userUuid: UUID) : this(
        veriffSessionResponse.verification.id,
        userUuid,
        veriffSessionResponse.verification.url,
        veriffSessionResponse.verification.vendorData,
        veriffSessionResponse.verification.host,
        veriffSessionResponse.status,
        false,
        ZonedDateTime.now()
    )
}
