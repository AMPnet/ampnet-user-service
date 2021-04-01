package com.ampnet.userservice.persistence.model

import com.ampnet.userservice.persistence.StringCryptoConverter
import com.ampnet.userservice.service.pojo.VeriffDocument
import javax.persistence.Convert
import javax.persistence.Embeddable

@Embeddable
class Document(

    @Convert(converter = StringCryptoConverter::class)
    var type: String?,

    @Convert(converter = StringCryptoConverter::class)
    var number: String?,

    @Convert(converter = StringCryptoConverter::class)
    var country: String?,

    @Convert(converter = StringCryptoConverter::class)
    var validUntil: String?,

    @Convert(converter = StringCryptoConverter::class)
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
        identyumDocumentModel.number?.value,
        identyumDocumentModel.issuingCountryCode.value,
        identyumDocumentModel.dateOfExpiry?.value,
        identyumDocumentModel.dateOfIssue?.value
    )
}
