package com.ampnet.userservice.persistence.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "role")
class Role(
    @Id
    val id: Int,

    @Column(nullable = false, length = 32)
    val name: String,

    @Column(nullable = false)
    val description: String
)
