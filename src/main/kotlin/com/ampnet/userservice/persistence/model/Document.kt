package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.service.pojo.VeriffDocument
import javax.persistence.Embeddable

@Embeddable
class Document(
    var type: String?,
    var number: String?,
    var country: String?,
    var validUntil: String?,
    var validFrom: String?
) {
    constructor(veriffDocument: VeriffDocument) : this(
        veriffDocument.type,
        veriffDocument.number,
        veriffDocument.country,
        veriffDocument.validUntil,
        veriffDocument.validFrom
    )

    constructor(identyumDocumentModel: com.ampnet.userservice.service.pojo.Document) : this(
        identyumDocumentModel.type.value,
        identyumDocumentModel.number.value,
        identyumDocumentModel.issuingCountryCode.value,
        identyumDocumentModel.dateOfExpiry.value,
        identyumDocumentModel.dateOfIssue.value
    )
}
