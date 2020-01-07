package com.ampnet.userservice.service

import com.ampnet.userservice.controller.pojo.request.CreateAdminUserRequest
import com.ampnet.userservice.enums.UserRoleType
import com.ampnet.userservice.persistence.model.User
import com.ampnet.userservice.service.pojo.UserCount
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AdminService {
    fun findAll(pageable: Pageable): Page<User>
    fun findByEmail(email: String, pageable: Pageable): Page<User>
    fun findByRole(role: UserRoleType, pageable: Pageable): Page<User>
    fun createUser(request: CreateAdminUserRequest): User
    fun changeUserRole(userUuid: UUID, role: UserRoleType): User
    fun countUsers(): UserCount
}
