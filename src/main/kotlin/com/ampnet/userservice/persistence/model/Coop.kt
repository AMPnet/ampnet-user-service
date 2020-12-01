package com.ampnet.userservice.persistence.model

import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "coop")
class Coop(

    @Id
    val identifier: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column
    var hostname: String?,

    @Column
    var config: String?,

    @Column(nullable = false)
    var logo: String

) {
    constructor(identifier: String, name: String, hostname: String?, config: String?, logo: String) : this(
        identifier,
        name,
        ZonedDateTime.now(),
        hostname,
        config,
        logo
    )
}
