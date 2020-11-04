package com.ampnet.userservice.service.impl

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
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
        logger.debug { "Creating coop for request: $request" }
        coopRepository.findByIdentifier(request.identifier)?.let {
            throw ResourceAlreadyExistsException(
                ErrorCode.COOP_EXISTS,
                "Coop with identifier: ${request.identifier} already exists"
            )
        }
        val config = request.config?.let { serializeConfig(request.config) }
        val coop = Coop(request.identifier, request.name, request.hostname, config)
        return coopRepository.save(coop)
    }

    @Transactional(readOnly = true)
    override fun getCoopByIdentifier(identifier: String): Coop? = coopRepository.findByIdentifier(identifier)

    @Transactional(readOnly = true)
    override fun getCoopByHost(host: String): Coop? = coopRepository.findByHostname(host)

    @Transactional
    override fun updateCoop(identifier: String, request: CoopUpdateRequest): Coop? {
        coopRepository.findByIdentifier(identifier)?.let { coop ->
            request.name?.let { coop.name = it }
            request.hostname?.let { coop.hostname = it }
            request.config?.let { coop.config = serializeConfig(it) }
            return coop
        }
        return null
    }

    private fun serializeConfig(config: Map<String, Any>) = objectMapper.writeValueAsString(config)
}
