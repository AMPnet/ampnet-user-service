package com.ampnet.userservice.service.impl

import com.ampnet.userservice.config.ApplicationProperties
import com.ampnet.userservice.controller.pojo.request.CoopRequest
import com.ampnet.userservice.controller.pojo.request.CoopUpdateRequest
import com.ampnet.userservice.exception.ErrorCode
import com.ampnet.userservice.exception.InternalException
import com.ampnet.userservice.exception.ResourceAlreadyExistsException
import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.service.CloudStorageService
import com.ampnet.userservice.service.CoopService
import com.ampnet.userservice.service.pojo.CoopServiceResponse
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

private val logger = KotlinLogging.logger {}

@Service
class CoopServiceImpl(
    private val coopRepository: CoopRepository,
    private val objectMapper: ObjectMapper,
    private val cloudStorageService: CloudStorageService,
    private val applicationProperties: ApplicationProperties
) : CoopService {

    @Transactional
    @Throws(ResourceAlreadyExistsException::class, InternalException::class)
    override fun createCoop(request: CoopRequest, logo: MultipartFile?, banner: MultipartFile?): CoopServiceResponse {
        logger.debug { "Creating coop for request: $request" }
        if (applicationProperties.coop.enableCreating.not()) {
            throw InternalException(ErrorCode.COOP_CREATING_DISABLED, "Creating new coop is disabled!")
        }
        coopRepository.findByIdentifier(request.identifier)?.let {
            throw ResourceAlreadyExistsException(
                ErrorCode.COOP_EXISTS,
                "Coop with identifier: ${request.identifier} already exists"
            )
        }
        val config = request.config?.let { serializeConfig(request.config) }
        val logoLink = logo?.let {
            cloudStorageService.saveFile(ServiceUtils.getImageNameFromMultipartFile(it), it.bytes)
        }
        val bannerLink = banner?.let {
            cloudStorageService.saveFile(ServiceUtils.getImageNameFromMultipartFile(it), it.bytes)
        }
        val coop = Coop(request.identifier, request.name, request.hostname, config, logoLink, bannerLink)
        return CoopServiceResponse(coopRepository.save(coop))
    }

    @Transactional(readOnly = true)
    override fun getCoopByIdentifier(identifier: String): CoopServiceResponse? =
        coopRepository.findByIdentifier(identifier)?.let { CoopServiceResponse(it) }

    @Transactional(readOnly = true)
    override fun getCoopByHostname(host: String): CoopServiceResponse? =
        coopRepository.findByHostname(host)?.let { CoopServiceResponse(it) }

    @Transactional
    @Throws(InternalException::class)
    override fun updateCoop(
        identifier: String,
        request: CoopUpdateRequest,
        logo: MultipartFile?,
        banner: MultipartFile?
    ): CoopServiceResponse? {
        coopRepository.findByIdentifier(identifier)?.let { coop ->
            request.name?.let { coop.name = it }
            request.hostname?.let { coop.hostname = it }
            request.config?.let { coop.config = serializeConfig(it) }
            request.needUserVerification?.let { coop.needUserVerification = it }
            logo?.let {
                coop.logo = cloudStorageService.saveFile(ServiceUtils.getImageNameFromMultipartFile(it), it.bytes)
            }
            banner?.let {
                coop.banner = cloudStorageService.saveFile(ServiceUtils.getImageNameFromMultipartFile(it), it.bytes)
            }
            request.kycProvider?.let { coop.kycProvider = it }
            request.sigUpEnabled?.let { coop.signUpEnabled = it }
            return CoopServiceResponse(coop)
        }
        return null
    }

    private fun serializeConfig(config: Map<String, Any>) = objectMapper.writeValueAsString(config)
}
