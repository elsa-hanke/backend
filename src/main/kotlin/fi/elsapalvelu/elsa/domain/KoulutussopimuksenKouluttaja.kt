package fi.elsapalvelu.elsa.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.time.LocalDate
import javax.persistence.*

@Entity
@Table(name = "koulutussopimuksen_kouluttaja")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class KoulutussopimuksenKouluttaja(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    var id: Long? = null,

    @ManyToOne
    var kouluttaja: Kayttaja? = null,

    @Column(name = "nimi", nullable = false)
    var nimi: String? = null,

    @Column(name = "nimike")
    var nimike: String? = null,

    @Column(name = "toimipaikka")
    var toimipaikka: String? = null,

    @Column(name = "lahiosoite")
    var lahiosoite: String? = null,

    @Column(name = "postitoimipaikka")
    var postitoimipaikka: String? = null,

    @Column(name = "puhelin")
    var puhelin: String? = null,

    @Column(name = "sahkoposti")
    var sahkoposti: String? = null,

    @Column(name = "sopimus_hyvaksytty")
    var sopimusHyvaksytty: Boolean = false,

    @Column(name = "kuittausaika")
    var kuittausaika: LocalDate? = null,

    @ManyToOne
    @JsonIgnoreProperties(value = ["koulutussopimuksenKouluttajat"], allowSetters = true)
    var koulutussopimus: KoejaksonKoulutussopimus? = null

) : Serializable {

    override fun hashCode() = 31

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KoulutussopimuksenKouluttaja) return false

        return id != null && other.id != null && id == other.id
    }

    override fun toString(): String {
        return "KoulutussopimuksenKouluttaja(" +
            "id=$id, " +
            "kouluttaja=$kouluttaja, " +
            "nimi=$nimi, " +
            "nimike=$nimike, " +
            "toimipaikka=$toimipaikka, " +
            "lahiosoite=$lahiosoite, " +
            "postitoiminpaikka=$postitoimipaikka, " +
            "puhelin=$puhelin, " +
            "sahkoposti=$sahkoposti, " +
            "sopimusHyvaksytty=$sopimusHyvaksytty, " +
            "sopimuksenKuittausaika=$kuittausaika, " +
            "koulutussopimus=$koulutussopimus)"
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
