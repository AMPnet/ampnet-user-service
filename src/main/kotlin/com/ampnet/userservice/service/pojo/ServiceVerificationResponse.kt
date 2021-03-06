package com.ampnet.userservice.service.pojo

import com.ampnet.userservice.persistence.model.VeriffDecision
import com.ampnet.userservice.persistence.model.VeriffSessionState

data class ServiceVerificationResponse(
    val verificationUrl: String,
    val state: String,
    val decision: ServiceVerificationDecision?
) {
    constructor(url: String, state: VeriffSessionState, decision: VeriffDecision? = null) : this(
        url, state.name.toLowerCase(), decision?.let { ServiceVerificationDecision(it) }
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
    constructor(decision: VeriffDecision) : this(
        decision.id,
        decision.status,
        decision.code,
        decision.reason,
        decision.reasonCode,
        decision.decisionTime,
        decision.acceptanceTime
    )
}
