package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO
import java.util.*

interface KoejaksonKoulutussopimusService {

    fun create(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        userId: String
    ): KoejaksonKoulutussopimusDTO

    fun update(
        koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO,
        userId: String
    ): KoejaksonKoulutussopimusDTO

    fun findOne(id: Long): Optional<KoejaksonKoulutussopimusDTO>

    fun delete(id: Long)
}
