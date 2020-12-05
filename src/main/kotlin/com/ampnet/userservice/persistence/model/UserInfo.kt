package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.service.pojo.VeriffDocument
import com.ampnet.userservice.service.pojo.VeriffPerson
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
@Suppress("LongParameterList")
class UserInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var sessionId: String,

    var idNumber: String?,

    @Column(nullable = false)
    var firstName: String,

    @Column(nullable = false)
    var lastName: String,

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
        0,
        sessionId,
        person.idNumber,
        person.firstName,
        person.lastName,
        person.dateOfBirth,
        Document(document),
        person.nationality,
        person.placeOfBirth,
        ZonedDateTime.now(),
        false,
        false
    )
}
