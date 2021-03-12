package com.ampnet.userservice.controller.pojo.response

import com.ampnet.userservice.service.pojo.UserResponse

data class UsersListResponse(val users: List<UserResponse>, val page: Int, val totalPages: Int)
