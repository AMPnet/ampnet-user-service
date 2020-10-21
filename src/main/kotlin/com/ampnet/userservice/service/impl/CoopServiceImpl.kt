package com.ampnet.userservice.service.impl

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.service.CoopService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}
private const val COOP_IDENTIFIER_MAX_SIZE = 64

@Service
class CoopServiceImpl(
    private val coopRepository: CoopRepository,
    private val objectMapper: ObjectMapper
) : CoopService {

    @Transactional
    override fun createCoop(request: CoopRequest): Coop {
        val identifier = request.name.trim().take(COOP_IDENTIFIER_MAX_SIZE).toLowerCase()
            .replace("\\s+".toRegex(), "-")
        logger.debug { "Creating coop with identifier: $identifier for request: $request" }
        val config = request.config?.let { serializeConfig(request.config) }
        val coop = Coop(identifier, request.name, request.host, config)
        return coopRepository.save(coop)
    }

    @Transactional(readOnly = true)
    override fun getCoopByIdentifier(identifier: String): Coop? = coopRepository.findByIdentifier(identifier)

    @Transactional(readOnly = true)
    override fun getCoopByHost(host: String): Coop? = coopRepository.findByHost(host)

    @Transactional
    override fun updateCoop(identifier: String, request: CoopUpdateRequest): Coop? {
        coopRepository.findByIdentifier(identifier)?.let { coop ->
            request.name?.let { coop.name = it }
            request.host?.let { coop.host = it }
            request.config?.let { coop.config = serializeConfig(it) }
            return coop
        }
        return null
    }

    private fun serializeConfig(config: Map<String, Any>) = objectMapper.writeValueAsString(config)
}
