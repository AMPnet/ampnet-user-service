package com.ampnet.userservice.exception

@Suppress("MagicNumber")
enum class VeriffVerificationCode(val code: Int) {
    POSITIVE(9001), NEGATIVE(9102), RESUBMITTED(9103), NEGATIVE_EXPIRED(9104);
    companion object {
        private val map = values().associateBy(VeriffVerificationCode::code)
        fun fromInt(type: Int) = map[type]
    }
}

@Suppress("MagicNumber")
enum class VeriffReasonCode(val code: Int) {
    PHOTO_MISSING(201), FACE_NOT_VISIBLE(202), DOC_NOT_VISIBLE(203), POOR_IMAGE(204),
    DOC_DAMAGED(205), DOC_TYPE_NOT_SUPPORTED(206), DOC_EXPIRED(207);
    companion object {
        private val map = values().associateBy(VeriffReasonCode::code)
        fun fromInt(type: Int?) = map[type]
    }
}
