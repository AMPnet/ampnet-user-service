package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.validation.EmailConstraint

data class MailCheckRequest(@field:EmailConstraint val email: String, val coop: String)
