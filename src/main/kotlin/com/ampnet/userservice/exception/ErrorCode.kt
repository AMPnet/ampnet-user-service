package com.ampnet.userservice.exception

enum class ErrorCode(val categoryCode: String, var specificCode: String, var message: String) {
    // Registration: 01
    REG_INCOMPLETE("01", "01", "Incomplete signup information"),
    REG_INVALID("01", "02", "Signup information complete but invalid"),
    REG_USER_EXISTS("01", "03", "Signup failed because user exists"),
    REG_EMAIL_INVALID_TOKEN("01", "04", "Failed Email confirmation, invalid token format"),
    REG_EMAIL_EXPIRED_TOKEN("01", "05", "Failed Email confirmation, token expired"),
    REG_SOCIAL("01", "06", "Social exception"),
    REG_IDENTYUM("01", "07", "Identyum exception"),
    REG_IDENTYUM_TOKEN("01", "08", "Identyum exception: failed to get token"),
    REG_IDENTYUM_EXISTS("01", "09", "UserInfo exists"),
    REG_RECAPTCHA("01", "10", "reCAPTCHA verification failed"),

    // Authentication: 02
    AUTH_INVALID_LOGIN_METHOD("02", "01", "Invalid login method"),
    AUTH_FORGOT_TOKEN_MISSING("02", "02", "Missing forgot password token"),
    AUTH_FORGOT_TOKEN_EXPIRED("02", "03", "Forgot password token expired"),

    // Users: 03
    USER_MISSING("03", "01", "Non existing user"),
    USER_BANK_INVALID("03", "02", "Invalid bank account data"),
    USER_DIFFERENT_PASSWORD("03", "03", "Different password"),
    USER_ROLE_INVALID("03", "04", "Invalid user role"),

    // Internal: 08
    INT_DB("08", "07", "Database exception"),
    INT_REQUEST("08", "08", "Invalid controller request exception"),

    // Coop: 10
    COOP_MISSING("10", "01", "Missing coop"),
    COOP_EXISTS("10", "02", "Coop already exists")
}
