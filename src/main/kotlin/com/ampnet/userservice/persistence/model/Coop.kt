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
    var host: String,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    var config: String?
) {
    constructor(identifier: String, name: String, host: String, config: String?) : this(
        identifier,
        name,
        host,
        ZonedDateTime.now(),
        config
    )
}
