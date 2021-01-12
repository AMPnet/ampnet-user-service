package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.service.pojo.IdentyumInput
import com.ampnet.userservice.service.pojo.VeriffPerson
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_info")
@Suppress("LongParameterList")
class UserInfo(
    @Id
    val uuid: UUID,

    @Column(nullable = false)
    var sessionId: String,

    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

    @Column
    var nationality: String?,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var connected: Boolean,

    @Column
    var identyumUserUuid: String?
) {
    constructor(sessionId: String, person: VeriffPerson) : this(
        UUID.randomUUID(),
        sessionId,
        person.firstName,
        person.lastName,
        person.nationality,
        ZonedDateTime.now(),
        false,
        null
    )

    constructor(identyum: IdentyumInput) : this(
        UUID.randomUUID(),
        identyum.clientSessionUuid.toString(),
        identyum.data.personalData.firstName.value,
        identyum.data.personalData.lastName.value,
        identyum.data.personalData.nationalityCode?.value,
        ZonedDateTime.now(),
        false,
        identyum.userUuid.toString()
    )
}
