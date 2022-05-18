package fi.elsapalvelu.elsa.repository

import fi.elsapalvelu.elsa.domain.KoejaksonVastuuhenkilonArvio
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface KoejaksonVastuuhenkilonArvioRepository :
    JpaRepository<KoejaksonVastuuhenkilonArvio, Long>,
    JpaSpecificationExecutor<KoejaksonVastuuhenkilonArvio> {

    fun findOneByIdAndVastuuhenkiloUserId(
        id: Long,
        userId: String
    ): Optional<KoejaksonVastuuhenkilonArvio>

    fun findByOpintooikeusId(opintooikeusId: Long): Optional<KoejaksonVastuuhenkilonArvio>

    fun findAllByVastuuhenkiloUserId(
        userId: String
    ): List<KoejaksonVastuuhenkilonArvio>

    fun findOneByIdAndOpintooikeusYliopistoId(
        id: Long,
        yliopistoId: Long
    ): Optional<KoejaksonVastuuhenkilonArvio>
}
