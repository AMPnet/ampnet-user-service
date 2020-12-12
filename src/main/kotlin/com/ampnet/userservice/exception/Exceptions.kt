package com.ampnet.userservice.exception

class InvalidLoginMethodException(exceptionMessage: String) : Exception(exceptionMessage)

class InvalidRequestException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

class ResourceAlreadyExistsException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class ResourceNotFoundException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class SocialException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

class RequestValidationException(exceptionMessage: String, val errors: Map<String, String>) :
    Exception(exceptionMessage)

class InternalException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class ReCaptchaException(
    exceptionMessage: String,
    throwable: Throwable? = null
) : Exception(exceptionMessage, throwable)

class VeriffException(exceptionMessage: String, throwable: Throwable? = null) : Exception(exceptionMessage, throwable)
