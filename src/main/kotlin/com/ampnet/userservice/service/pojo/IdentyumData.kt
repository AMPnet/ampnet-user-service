package com.ampnet.userservice.service.pojo

import java.util.UUID

data class IdentyumInput(
    val clientSessionUuid: UUID,
    val userSessionUuid: UUID,
    val userUuid: UUID,
    val reportUuid: UUID,
    val client: String,
    val status: String,
    val ordinal: Long,
    val data: Data,
    // images
    // customParameters
    // clientParameters
    val timeCreated: String
) {
    override fun toString(): String {
        return "IdentyumInput(clientSessionUuid: $clientSessionUuid, userSessionUuid: $userSessionUuid, " +
            "userUuid: $userUuid, reportUuid: $reportUuid, client: $client, status: $status, ordinal:$ordinal, " +
            "timeCreated: $timeCreated)"
    }
}

data class Data(
    val personalData: PersonalData
)

data class PersonalData(
    val firstName: ValueSource,
    val lastName: ValueSource,
    val nationalityCode: ValueSource,
    val dateOfBirth: ValueSource,
    val sex: ValueSource,
    val adresses: List<ValuesSources>,
    val personalNumbers: List<ValuesSources>,
    val emails: List<ValuesSources>,
    val phones: List<ValuesSources>,
    val documents: List<Document>
)

data class Source(
    val id: Long,
    val value: String,
    val similarity: Long,
    val verified: Boolean,
    val timeCreated: String
)
data class ValueSource(
    val value: String,
    val sources: List<Source>
)

data class ValueType(
    val type: String,
    val value: String
)
data class SourceValueType(
    val id: Long,
    val value: ValueType,
    val similarity: Long,
    val verified: Boolean,
    val timeCreated: String
)
data class ValuesSources(
    val value: ValueType,
    val sources: List<SourceValueType>
)

data class Document(
    val frontImageUuid: ValueSource,
    val backImageUuid: ValueSource,
    val signatureImageUuid: ValueSource,
    val dateOfExpiry: ValueSource,
    val type: ValueSource,
    val issuingCountryCode: ValueSource,
    val number: ValueSource,
    val issuedBy: ValueSource
)
