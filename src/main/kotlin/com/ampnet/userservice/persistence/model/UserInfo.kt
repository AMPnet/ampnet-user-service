package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.service.pojo.IdentyumInput
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_info")
data class UserInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var clientSessionUuid: String,

    @Column(nullable = false)
    var identyumUserUuid: String,

    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

    @Column(nullable = false)
    var verifiedEmail: String,

    @Column(nullable = false)
    var phoneNumber: String,

    @Column(nullable = false)
    var dateOfBirth: String,

    @Column(nullable = false)
    var personalNumber: String,

    @Embedded
    var document: Document,

    @Column(nullable = false)
    var nationality: String,

    @Column(nullable = false)
    var address: String,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var connected: Boolean,

    @Column(nullable = false)
    var deactivated: Boolean
) {
    constructor(identyum: IdentyumInput) : this(
        0,
        identyum.clientSessionUuid.toString(),
        identyum.userUuid.toString(),
        identyum.data.personalData.firstName.value,
        identyum.data.personalData.lastName.value,
        identyum.data.personalData.emails.first().value.value,
        identyum.data.personalData.phones.first().value.value,
        identyum.data.personalData.dateOfBirth.value,
        identyum.data.personalData.personalNumbers.first().value.value,
        Document(identyum.data.personalData.documents.first()),
        identyum.data.personalData.nationalityCode.value,
        identyum.data.personalData.adresses.first().value.value,
        ZonedDateTime.now(),
        false,
        false
    )
}
