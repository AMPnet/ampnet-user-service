package com.ampnet.userservice.service.pojo

import com.ampnet.userservice.controller.pojo.request.SignupRequestUserInfo
import com.ampnet.userservice.enums.AuthMethod
import com.ampnet.userservice.validation.EmailConstraint
import com.ampnet.userservice.validation.PasswordConstraint
import javax.validation.constraints.NotNull

data class CreateUserServiceRequest(

    @field:NotNull
    val firstName: String,

    @field:NotNull
    val lastName: String,

    @field:EmailConstraint
    @field:NotNull
    val email: String,

    @field:PasswordConstraint
    val password: String?,

    @field:NotNull
    val authMethod: AuthMethod,

    val coop: String?
) {
    constructor(request: SignupRequestUserInfo, coop: String?) : this(
        request.firstName,
        request.lastName,
        request.email,
        request.password,
        AuthMethod.EMAIL,
        coop
    )

    constructor(socialUser: SocialUser, authMethod: AuthMethod, coop: String?) : this(
        socialUser.firstName,
        socialUser.lastName,
        socialUser.email,
        null,
        authMethod,
        coop
    )
}
