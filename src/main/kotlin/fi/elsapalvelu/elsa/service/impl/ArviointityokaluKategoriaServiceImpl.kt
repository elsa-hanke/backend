package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.repository.ArviointityokaluKategoriaRepository
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluKategoriaMapper
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluKategoriaService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class ArviointityokaluKategoriaServiceImpl(
    private val arviointityokaluKategoriaRepository: ArviointityokaluKategoriaRepository,
    private val arviointityokaluKategoriaMapper: ArviointityokaluKategoriaMapper
) : ArviointityokaluKategoriaService {

    override fun create(arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO): ArviointityokaluKategoriaDTO {
        var arviointityokalu = arviointityokaluKategoriaMapper.toEntity(arviointityokaluKategoriaDTO)
        arviointityokalu.luontiaika = Instant.now()
        arviointityokalu.muokkausaika = Instant.now()
        arviointityokalu = arviointityokaluKategoriaRepository.save(arviointityokalu)
        return arviointityokaluKategoriaMapper.toDto(arviointityokalu)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ArviointityokaluKategoriaDTO> {
        return arviointityokaluKategoriaRepository.findAll()
            .map(arviointityokaluKategoriaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<ArviointityokaluKategoriaDTO> {
        return arviointityokaluKategoriaRepository.findById(id)
            .map(arviointityokaluKategoriaMapper::toDto)
    }

    override fun delete(id: Long) {
        arviointityokaluKategoriaRepository.deleteById(id)
    }
}
