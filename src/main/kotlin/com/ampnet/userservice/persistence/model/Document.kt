package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.service.pojo.Document
import javax.persistence.Embeddable

@Embeddable
data class Document(
    var type: String,
    var number: String,
    var dateOfExpiry: String,
    var issuingCountry: String,
    var issuedBy: String
) {
    constructor(document: Document) : this(
        document.type.value,
        document.number.value,
        document.dateOfExpiry.value,
        document.issuingCountryCode.value,
        document.issuedBy.value
    )
}
