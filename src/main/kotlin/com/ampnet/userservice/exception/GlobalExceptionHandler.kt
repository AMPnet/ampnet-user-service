package com.ampnet.userservice.exception

import mu.KLogging
import org.springframework.core.NestedExceptionUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Suppress("TooManyFunctions")
class GlobalExceptionHandler {

    companion object : KLogging()

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleResourceAlreadyExists(exception: ResourceAlreadyExistsException): ErrorResponse {
        logger.info("ResourceAlreadyExistsException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceDoesNotExists(exception: ResourceNotFoundException): ErrorResponse {
        logger.warn("ResourceNotFoundException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(exception: InvalidRequestException): ErrorResponse {
        logger.info("InvalidRequestException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(SocialException::class)
    fun handleSocialException(exception: SocialException): ErrorResponse {
        logger.info("SocialException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(IdentyumCommunicationException::class)
    fun handleInternalException(exception: IdentyumCommunicationException): ErrorResponse {
        logger.warn("IdentyumCommunicationException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidLoginMethodException::class)
    fun handleInvalidLoginMethod(exception: InvalidLoginMethodException): ErrorResponse {
        logger.warn("InvalidRequestException", exception)
        return generateErrorResponse(ErrorCode.AUTH_INVALID_LOGIN_METHOD, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDbException(exception: DataIntegrityViolationException): ErrorResponse {
        logger.warn("DataIntegrityViolationException", exception)
        val message = NestedExceptionUtils.getMostSpecificCause(exception).message
        return generateErrorResponse(ErrorCode.INT_DB, message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(exception: MethodArgumentNotValidException): ErrorResponse {
        val errors = mutableMapOf<String, String>()
        val sb = StringBuilder()
        exception.bindingResult.allErrors.forEach { error ->
            val filed = (error as FieldError).field
            val errorMessage = error.defaultMessage ?: "Unknown"
            errors[filed] = errorMessage
            sb.append("$filed $errorMessage. ")
        }
        logger.info { "MethodArgumentNotValidException: $sb" }
        return generateErrorResponse(ErrorCode.INT_REQUEST, sb.toString(), errors)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RequestValidationException::class)
    fun handleRequestExceptions(exception: RequestValidationException): ErrorResponse {
        return generateErrorResponse(ErrorCode.INT_REQUEST, exception.message, exception.errors)
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalException::class)
    fun handleInternalExceptions(exception: InternalException): ErrorResponse {
        logger.error("InternalException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    private fun generateErrorResponse(
        errorCode: ErrorCode,
        systemMessage: String?,
        errors: Map<String, String> = emptyMap()
    ): ErrorResponse {
        val errorMessage = systemMessage ?: "Error not defined"
        val errCode = errorCode.categoryCode + errorCode.specificCode
        return ErrorResponse(errorCode.message, errCode, errorMessage, errors)
    }
}
