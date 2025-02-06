package fi.elsapalvelu.elsa.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.Instant

@Entity
@Table(name = "arviointityokalu")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class Arviointityokalu(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get: NotNull
    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @NotNull
    @ManyToOne(optional = true)
    var kayttaja: Kayttaja? = null,

    @ManyToOne(optional = false)
    var kategoria: ArviointityokaluKategoria? = null,

    @NotNull
    @Column(name = "kaytossa")
    var kaytossa: Boolean = true,

    @get: NotNull
    @Column(name = "luontiaika", nullable = false)
    var luontiaika: Instant? = null,

    @get: NotNull
    @Column(name = "muokkausaika", nullable = false)
    var muokkausaika: Instant? = null,

    ) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Arviointityokalu) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Arviointityokalu{" +
        "id=$id" +
        ", nimi='$nimi'" +
        ", kayttaja='$kayttaja'" +
        ", luontiaika='$luontiaika'" +
        ", muokkausaika='$muokkausaika'" +
        "}"

    companion object {
        private const val serialVersionUID = 1L
    }
}
