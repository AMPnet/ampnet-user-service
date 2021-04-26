package com.ampnet.userservice.service.pojo

import com.ampnet.userservice.persistence.model.User
import java.time.ZonedDateTime

data class VeriffSessionRequest(
    val verification: VeriffSessionVerificationRequest
) {
    constructor(user: User, callback: String) : this(VeriffSessionVerificationRequest(user, callback))
}

data class VeriffSessionVerificationRequest(
    val callback: String,
    val person: VeriffSessionPerson,
    val vendorData: String,
    val timestamp: ZonedDateTime
) {
    constructor(user: User, callback: String) : this(
        callback,
        VeriffSessionPerson(user.firstName, user.lastName),
        user.uuid.toString(),
        ZonedDateTime.now()
    )
}

data class VeriffSessionPerson(
    val firstName: String?,
    val lastName: String?
)
