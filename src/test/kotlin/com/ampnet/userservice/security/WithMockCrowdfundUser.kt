package com.ampnet.userservice.security

import com.ampnet.userservice.COOP
import com.ampnet.userservice.enums.PrivilegeType
import com.ampnet.userservice.enums.UserRole
import org.springframework.security.test.context.support.WithSecurityContext

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@WithSecurityContext(factory = WithMockUserSecurityFactory::class)
annotation class WithMockCrowdfundUser(
    val uuid: String = "8a733721-9bb3-48b1-90b9-6463ac1493eb",
    val email: String = "user@email.com",
    val role: UserRole = UserRole.USER,
    val privileges: Array<PrivilegeType> = [],
    val enabled: Boolean = true,
    val verified: Boolean = true,
    val coop: String = COOP
)
