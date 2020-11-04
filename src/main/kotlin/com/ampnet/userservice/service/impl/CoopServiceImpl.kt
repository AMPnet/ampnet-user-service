package com.ampnet.userservice.service.impl

import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.service.CoopService
import com.ampnet.userservice.service.pojo.CoopServiceResponse
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class CoopServiceImpl(
    private val coopRepository: CoopRepository,
    private val objectMapper: ObjectMapper
) : CoopService {

    @Transactional
    override fun createCoop(request: CoopRequest): CoopServiceResponse {
        logger.debug { "Creating coop for request: $request" }
        coopRepository.findByIdentifier(request.identifier)?.let {
            throw ResourceAlreadyExistsException(
                ErrorCode.COOP_EXISTS,
                "Coop with identifier: ${request.identifier} already exists"
            )
        }
        val config = request.config?.let { serializeConfig(request.config) }
        val coop = Coop(request.identifier, request.name, request.hostname, config)
        return CoopServiceResponse(coopRepository.save(coop))
    }

    @Transactional(readOnly = true)
    override fun getCoopByIdentifier(identifier: String): CoopServiceResponse? =
        coopRepository.findByIdentifier(identifier)?.let { CoopServiceResponse(it) }

    @Transactional(readOnly = true)
    override fun getCoopByHostname(host: String): CoopServiceResponse? =
        coopRepository.findByHostname(host)?.let { CoopServiceResponse(it) }

    @Transactional
    override fun updateCoop(identifier: String, request: CoopUpdateRequest): CoopServiceResponse? {
        coopRepository.findByIdentifier(identifier)?.let { coop ->
            request.name?.let { coop.name = it }
            request.hostname?.let { coop.hostname = it }
            request.config?.let { coop.config = serializeConfig(it) }
            return CoopServiceResponse(coop)
        }
        return null
    }

    private fun serializeConfig(config: Map<String, Any>) = objectMapper.writeValueAsString(config)
}
