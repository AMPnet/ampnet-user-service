package com.ampnet.userservice.persistence.repository

import com.ampnet.userservice.persistence.model.VeriffDecision
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface VeriffDecisionRepository: JpaRepository<VeriffDecision, String> {
    @Modifying
    @Query("DELETE FROM VeriffDecision decision WHERE decision.id = :id")
    fun deleteByIdIfPresent(id: String)
}
