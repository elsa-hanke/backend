package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.service.dto.ArviointityokaluKategoriaDTO
import java.util.*

interface ArviointityokaluKategoriaService {

    fun create(arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO): ArviointityokaluKategoriaDTO

    fun update(arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO): ArviointityokaluKategoriaDTO?

    fun findAll(): List<ArviointityokaluKategoriaDTO>

    fun findOne(id: Long): Optional<ArviointityokaluKategoriaDTO>

    fun delete(id: Long)

}
