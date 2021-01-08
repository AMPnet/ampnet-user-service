package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.service.pojo.IdentyumInput
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "identyum_user_info")
@Suppress("LongParameterList")
class IdentyumUserInfo(
    @Id
    val uuid: UUID,

    @Column(nullable = false)
    val clientSessionUuid: String,

    @Column(nullable = false)
    val identyumUserUuid: String,

    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

    var idNumber: String?,

    var dateOfBirth: String?,

    @Embedded
    var document: Document,

    var nationality: String?,

    var placeOfBirth: String?,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var connected: Boolean,

    @Column(nullable = false)
    var deactivated: Boolean
) {
    constructor(identyum: IdentyumInput) : this(
        UUID.randomUUID(),
        identyum.clientSessionUuid.toString(),
        identyum.userUuid.toString(),
        identyum.data.personalData.firstName.value,
        identyum.data.personalData.lastName.value,
        identyum.data.personalData.personalNumbers.firstOrNull()?.value?.value,
        identyum.data.personalData.dateOfBirth.value,
        Document(identyum.data.personalData.documents.first()),
        identyum.data.personalData.nationalityCode.value,
        identyum.data.personalData.adresses.first().value.value,
        ZonedDateTime.now(),
        false,
        false
    )
}
