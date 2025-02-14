package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import jakarta.persistence.*
import jakarta.validation.constraints.NotNull

@Entity
@Table(name = "asiakirja_data")
@Cache(usage = CacheConcurrencyStrategy.NONE)
data class AsiakirjaData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @Column(name = "data", nullable = false, columnDefinition = "clob")
    var data: ByteArray? = null

) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AsiakirjaData) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "AsiakirjaData{id=$id}"

    companion object {
        private const val serialVersionUID = 1L
    }

}
