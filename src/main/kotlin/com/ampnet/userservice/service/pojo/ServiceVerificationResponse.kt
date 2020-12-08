package com.ampnet.userservice.service.pojo

data class ServiceVerificationResponse(
    val verificationUrl: String,
    val decision: ServiceVerificationDecision?
) {
    constructor(url: String, verification: VeriffVerification? = null) : this(
        url, verification?.let { ServiceVerificationDecision(it) }
    )
}

data class ServiceVerificationDecision(
    val sessionId: String,
    val status: VeriffStatus,
    val code: Int,
    val reason: String?,
    val reasonCode: Int?,
    val decisionTime: String?,
    val acceptanceTime: String?
) {
    constructor(verification: VeriffVerification) : this(
        verification.id,
        verification.status,
        verification.code,
        verification.reason,
        verification.reasonCode,
        verification.decisionTime,
        verification.acceptanceTime
    )
}
