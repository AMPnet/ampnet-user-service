package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.validation.EmailConstraint

data class MailCheckRequest(@EmailConstraint val email: String)
