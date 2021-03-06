package com.ampnet.userservice.validation

import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Size
import kotlin.reflect.KClass

@MustBeDocumented
@ReportAsSingleViolation
@Constraint(validatedBy = [])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Size(min = 8, max = 64)
annotation class PasswordConstraint constructor(

    val message: String = "Invalid Password",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []
)
