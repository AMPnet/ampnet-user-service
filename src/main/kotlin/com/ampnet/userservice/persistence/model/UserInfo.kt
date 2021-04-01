package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.service.pojo.IdentyumInput
import com.ampnet.userservice.service.pojo.VeriffDocument
import com.ampnet.userservice.service.pojo.VeriffPerson
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Embedded
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
    var idNumber: String?,

    @Column
    var dateOfBirth: String?,

    @Embedded
    var document: Document,

    @Column
    var nationality: String?,

    @Column
    var placeOfBirth: String?,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var connected: Boolean,

    @Column(nullable = false)
    var deactivated: Boolean,

    @Column
    var identyumUserUuid: String?
) {
    constructor(sessionId: String, person: VeriffPerson, document: VeriffDocument) : this(
        UUID.randomUUID(),
        sessionId,
        person.firstName,
        person.lastName,
        person.idNumber,
        person.dateOfBirth,
        Document(document),
        person.nationality,
        person.placeOfBirth,
        ZonedDateTime.now(),
        false,
        false,
        null
    )

    constructor(identyum: IdentyumInput) : this(
        UUID.randomUUID(),
        identyum.clientSessionUuid.toString(),
        identyum.data.personalData.firstName.value,
        identyum.data.personalData.lastName.value,
        identyum.data.personalData.personalNumbers?.firstOrNull()?.value?.value,
        identyum.data.personalData.dateOfBirth.value,
        Document(identyum.data.personalData.documents.first()),
        identyum.data.personalData.nationalityCode?.value,
        identyum.data.personalData.adresses?.firstOrNull()?.value?.value,
        ZonedDateTime.now(),
        false,
        false,
        identyum.userUuid.toString()
    )
}
