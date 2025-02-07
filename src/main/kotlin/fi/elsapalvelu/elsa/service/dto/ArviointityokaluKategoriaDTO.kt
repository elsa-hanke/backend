package fi.elsapalvelu.elsa.service.dto

import jakarta.validation.constraints.NotNull
import java.io.Serializable
import java.util.*

data class ArviointityokaluKategoriaDTO(

    var id: Long? = null,

    @get: NotNull
    var nimi: String? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArviointityokaluKategoriaDTO) return false
        if (this.id == null) {
            return false
        }
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode() = Objects.hash(this.id)
}
