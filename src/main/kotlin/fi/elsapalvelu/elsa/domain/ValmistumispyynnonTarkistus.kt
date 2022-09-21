package fi.elsapalvelu.elsa.domain

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.envers.Audited
import org.hibernate.envers.RelationTargetAuditMode
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Audited
@Table(name = "valmistumispyynnon_tarkistus")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class ValmistumispyynnonTarkistus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    var valmistumispyynto: Valmistumispyynto? = null,

    @Column(name = "yek_suoritettu")
    var yekSuoritettu: Boolean? = null,

    @Column(name = "yek_suorituspaiva")
    var yekSuorituspaiva: LocalDate? = null,

    @Column(name = "ptl_suoritettu")
    var ptlSuoritettu: Boolean? = null,

    @Column(name = "ptl_suorituspaiva")
    var ptlSuorituspaiva: LocalDate? = null,

    @Column(name = "aiempi_el_koulutus_suoritettu")
    var aiempiElKoulutusSuoritettu: Boolean? = null,

    @Column(name = "aiempi_el_koulutus_suorituspaiva")
    var aiempiElKoulutusSuorituspaiva: LocalDate? = null,

    @Column(name = "lt_tutkinto_suoritettu")
    var ltTutkintoSuoritettu: Boolean? = null,

    @Column(name = "lt_tutkinto_suorituspaiva")
    var ltTutkintoSuorituspaiva: LocalDate? = null,

    @Column(name = "yliopistosairaalan_ulkopuolinen_tyo_tarkistettu")
    var yliopistosairaalanUlkopuolinenTyoTarkistettu: Boolean? = null,

    @Column(name = "yliopistosairaalatyo_tarkistettu")
    var yliopistosairaalatyoTarkistettu: Boolean? = null,

    @Column(name = "kokonaistyoaika_tarkistettu")
    var kokonaistyoaikaTarkistettu: Boolean? = null,

    @Column(name = "teoriakouluitus_tarkistettu")
    var teoriakoulutusTarkistettu: Boolean? = null,

    @Column(name = "kommentit_virkailijoille")
    var kommentitVirkailijoille: String? = null,

    @NotNull
    @Column(name = "muokkauspaiva", nullable = false)
    var muokkauspaiva: LocalDate? = null

) : Serializable {

    @PrePersist
    protected fun onCreate() {
        muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
    }

    @PreUpdate
    protected fun onUpdate() {
        muokkauspaiva = LocalDate.now(ZoneId.systemDefault())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValmistumispyynnonTarkistus) return false

        return id != null && other.id != null && id == other.id
    }

    override fun hashCode() = 31

    override fun toString(): String {
        return "ValmistumispyyntoVirkailija(id=$id, valmistumispyynto=$valmistumispyynto, yekSuoritettu=$yekSuoritettu, yekSuorituspaiva=$yekSuorituspaiva, ptlSuoritettu=$ptlSuoritettu, ptlSuorituspaiva=$ptlSuorituspaiva, aiempiElKoulutusSuoritettu=$aiempiElKoulutusSuoritettu, aiempiElKoulutusSuorituspaiva=$aiempiElKoulutusSuorituspaiva, ltTutkintoSuoritettu=$ltTutkintoSuoritettu, ltTutkintoSuorituspaiva=$ltTutkintoSuorituspaiva, yliopistosairaalanUlkopuolinenTyoTarkistettu=$yliopistosairaalanUlkopuolinenTyoTarkistettu, yliopistosairaalatyoTarkistettu=$yliopistosairaalatyoTarkistettu, kokonaistyoaikaTarkistettu=$kokonaistyoaikaTarkistettu, teoriakoulutusTarkistettu=$teoriakoulutusTarkistettu, kommentitVirkailijoille=$kommentitVirkailijoille, muokkauspaiva=$muokkauspaiva)"
    }
}

