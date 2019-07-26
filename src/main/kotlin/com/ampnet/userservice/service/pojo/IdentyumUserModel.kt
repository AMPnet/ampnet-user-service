package com.ampnet.userservice.service.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class IdentyumUserModel(
    @JsonProperty("identyumUuid")
    val identyumUuid: String,

    val emails: List<IdentyumEmailModel>,

    val phones: List<IdentyumPhoneModel>,

    val document: List<IdentyumDocumentModel>
)

data class IdentyumEmailModel(
    val type: String,

    val email: String
)

data class IdentyumPhoneModel(
    val type: String,

    @JsonProperty("phoneNumber")
    val phoneNumber: String
)

data class IdentyumDocumentModel(
    val type: String,

    @JsonProperty("countryCode")
    val countryCode: String,

    @JsonProperty("firstName")
    val firstName: String,

    @JsonProperty("lastName")
    val lastName: String,

    @JsonProperty("docNumber")
    val docNumber: String,

    val citizenship: String,

    val address: IdentyumAddressModel?,

    @JsonProperty("issuingAuthority")
    val issuingAuthority: String,

    @JsonProperty("personalIdentificationNumber")
    val personalIdentificationNumber: IdentyumPersonalIdModel,

    val resident: Boolean,

    @JsonProperty("documentBilingual")
    val documentBilingual: Boolean,

    val permanent: Boolean,

    @JsonProperty("docFrontImg")
    val docFrontImg: String?, // base64 encoded

    @JsonProperty("docBackImg")
    val docBackImg: String?, // base64 encoded

    @JsonProperty("docFaceImg")
    val docFaceImg: String?, // base64 encoded

    @JsonProperty("dateOfBirth")
    val dateOfBirth: String,

    @JsonProperty("dateOfExpiry")
    val dateOfExpiry: String,

    @JsonProperty("dateOfIssue")
    val dateOfIssue: String
)

data class IdentyumAddressModel(
    val city: String?,

    val county: String?,

    @JsonProperty("streetAndNumber")
    val streetAndNumber: String?
)

data class IdentyumPersonalIdModel(
    val type: String,

    val value: String
)
