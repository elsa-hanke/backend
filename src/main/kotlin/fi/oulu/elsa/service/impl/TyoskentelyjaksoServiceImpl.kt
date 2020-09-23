package fi.oulu.elsa.service.impl

import fi.oulu.elsa.domain.Tyoskentelyjakso
import fi.oulu.elsa.repository.TyoskentelyjaksoRepository
import fi.oulu.elsa.service.TyoskentelyjaksoService
import fi.oulu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.oulu.elsa.service.mapper.TyoskentelyjaksoMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Service Implementation for managing [Tyoskentelyjakso].
 */
@Service
@Transactional
class TyoskentelyjaksoServiceImpl(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val tyoskentelyjaksoMapper: TyoskentelyjaksoMapper
) : TyoskentelyjaksoService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun save(tyoskentelyjaksoDTO: TyoskentelyjaksoDTO): TyoskentelyjaksoDTO {
        log.debug("Request to save Tyoskentelyjakso : $tyoskentelyjaksoDTO")

        var tyoskentelyjakso = tyoskentelyjaksoMapper.toEntity(tyoskentelyjaksoDTO)
        tyoskentelyjakso = tyoskentelyjaksoRepository.save(tyoskentelyjakso)
        return tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
    }

    @Transactional(readOnly = true)
    override fun findAll(): MutableList<TyoskentelyjaksoDTO> {
        log.debug("Request to get all Tyoskentelyjaksos")
        return tyoskentelyjaksoRepository.findAll()
            .mapTo(mutableListOf(), tyoskentelyjaksoMapper::toDto)
    }

    /**
     *  Get all the tyoskentelyjaksos where Tyoskentelypaikka is `null`.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    override fun findAllWhereTyoskentelypaikkaIsNull(): MutableList<TyoskentelyjaksoDTO> {
        log.debug("Request to get all tyoskentelyjaksos where Tyoskentelypaikka is null")
        return tyoskentelyjaksoRepository.findAll()
            .asSequence()
            .filter { it.tyoskentelypaikka == null }
            .mapTo(mutableListOf()) { tyoskentelyjaksoMapper.toDto(it) }
    }

    /**
     *  Get all the tyoskentelyjaksos where Arviointi is `null`.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    override fun findAllWhereArviointiIsNull(): MutableList<TyoskentelyjaksoDTO> {
        log.debug("Request to get all tyoskentelyjaksos where Arviointi is null")
        return tyoskentelyjaksoRepository.findAll()
            .asSequence()
            .filter { it.arviointi == null }
            .mapTo(mutableListOf()) { tyoskentelyjaksoMapper.toDto(it) }
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<TyoskentelyjaksoDTO> {
        log.debug("Request to get Tyoskentelyjakso : $id")
        return tyoskentelyjaksoRepository.findById(id)
            .map(tyoskentelyjaksoMapper::toDto)
    }

    override fun delete(id: Long) {
        log.debug("Request to delete Tyoskentelyjakso : $id")

        tyoskentelyjaksoRepository.deleteById(id)
    }
}
