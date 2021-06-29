package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.Suoritusarviointi
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface SuoritusarviointiRepository :
    JpaRepository<Suoritusarviointi, Long>,
    JpaSpecificationExecutor<Suoritusarviointi> {

    fun findOneById(id: Long): Optional<Suoritusarviointi>

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariId(
        id: Long,
        pageable: Pageable
    ): Page<Suoritusarviointi>

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String,
        pageable: Pageable
    ): Page<Suoritusarviointi>

    fun findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        userId: String
    ): List<Suoritusarviointi>

    fun findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        id: Long,
        userId: String
    ): Optional<Suoritusarviointi>

    fun findOneByIdAndArvioinninAntajaUserId(
        id: Long,
        userId: String
    ): Optional<Suoritusarviointi>

    @Transactional
    @Modifying
    @Query("update Suoritusarviointi s set s.arvioinninAntaja.id = :newKayttaja where s.arvioinninAntaja.id = :currentKayttaja")
    fun changeKouluttaja(currentKayttaja: Long, newKayttaja: Long)
}
