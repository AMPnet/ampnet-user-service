package com.ampnet.userservice.persistence.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "coop_config")
data class CoopConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int

    // Ask Pevex for data
)
