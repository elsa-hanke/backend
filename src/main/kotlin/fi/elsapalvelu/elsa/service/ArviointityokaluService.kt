package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.ArviointityokaluDTO
import java.util.*

interface ArviointityokaluService {

    fun save(arviointityokaluDTO: ArviointityokaluDTO): ArviointityokaluDTO

    fun findAll(): List<ArviointityokaluDTO>

    fun findAllByKayttajaUserId(userId: String): MutableList<ArviointityokaluDTO>

    fun findOne(id: Long): Optional<ArviointityokaluDTO>

    fun delete(id: Long)

}
