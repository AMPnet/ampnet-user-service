package com.ampnet.userservice.persistence.model

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
        false
    )
}
