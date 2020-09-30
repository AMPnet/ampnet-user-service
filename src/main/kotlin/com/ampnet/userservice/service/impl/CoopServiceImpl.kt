package com.ampnet.userservice.service.impl

import com.ampnet.userservice.persistence.model.Coop
import com.ampnet.userservice.persistence.repository.CoopRepository
import com.ampnet.userservice.service.CoopService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}
private const val COOP_IDENTIFIER_MAX_SIZE = 64

@Service
class CoopServiceImpl(private val coopRepository: CoopRepository) : CoopService {

    @Transactional
    override fun createCoop(name: String): Coop {
        val identifier = name.trim().take(COOP_IDENTIFIER_MAX_SIZE).toLowerCase().replace("\\s+".toRegex(), "-")
        logger.debug { "Creating coop: $name with identifier: $identifier" }
        val coop = Coop(identifier, name)
        return coopRepository.save(coop)
    }

    @Transactional(readOnly = true)
    override fun getCoopByIdentifier(identifier: String): Coop? {
        return ServiceUtils.wrapOptional(coopRepository.findByIdentifier(identifier))
    }
}
