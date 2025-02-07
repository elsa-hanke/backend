package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.ArviointityokaluKategoria
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArviointityokaluKategoriaRepository : JpaRepository<ArviointityokaluKategoria, Long> {

    fun findAllByIdIn(ids: List<Long?>): MutableSet<ArviointityokaluKategoria>?

}
