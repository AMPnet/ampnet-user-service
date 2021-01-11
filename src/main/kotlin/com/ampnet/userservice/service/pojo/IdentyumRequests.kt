package com.ampnet.userservice.service.pojo

import java.util.UUID

data class IdentyumTokenRequest(val username: String, val password: String)
data class IdentyumInitRequest(val customParameters: IdentyumCustomParameters?)
data class IdentyumCustomParameters(val user: UUID?)
