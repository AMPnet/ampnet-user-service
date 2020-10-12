package com.ampnet.userservice.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
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
    val createdAt: ZonedDateTime,

    @Column(nullable = false)
    var url: String,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coop_config_id")
    var config: CoopConfig?
) {
    constructor(identifier: String, name: String, url: String = "") : this(
        0, identifier, name, ZonedDateTime.now(), url, null
    )
}
