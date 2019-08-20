package com.ampnet.userservice.controller.pojo.request

import com.ampnet.userservice.validation.EmailConstraint

data class TestUserSignupRequest(@EmailConstraint val email: String, val password: String)
