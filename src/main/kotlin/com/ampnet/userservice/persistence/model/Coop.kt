package com.ampnet.userservice.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "coop")
data class Coop(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val identifier: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    constructor(identifier: String, name: String) : this(
        0, identifier, name, ZonedDateTime.now()
    )
}
