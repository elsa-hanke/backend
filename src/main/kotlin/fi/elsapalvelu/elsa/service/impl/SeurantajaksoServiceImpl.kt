package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.Seurantajakso
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.SeurantajaksoService
import fi.elsapalvelu.elsa.service.dto.SeurantajaksoDTO
import fi.elsapalvelu.elsa.service.mapper.SeurantajaksoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityNotFoundException


@Service
@Transactional
class SeurantajaksoServiceImpl(
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val seurantajaksoMapper: SeurantajaksoMapper,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritemerkintaRepository: SuoritemerkintaRepository,
    private val kayttajaRepository: KayttajaRepository,
    private val koulutusjaksoRepository: KoulutusjaksoRepository,
    private val mailService: MailService
) : SeurantajaksoService {

    override fun create(
        seurantajaksoDTO: SeurantajaksoDTO,
        userId: String
    ): SeurantajaksoDTO {
        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)
        var seurantajakso = seurantajaksoMapper.toEntity(seurantajaksoDTO)
        seurantajakso.erikoistuvaLaakari = kirjautunutErikoistuvaLaakari
        seurantajakso = seurantajaksoRepository.save(seurantajakso)

        // Lukitaan seurantajaksoon liittyvät tiedot
        val arvioinnit =
            suoritusarviointiRepository.findForSeurantajakso(
                userId,
                seurantajakso.alkamispaiva!!,
                seurantajakso.paattymispaiva!!
            )
        arvioinnit.forEach { it.lukittu = true }
        suoritusarviointiRepository.saveAll(arvioinnit)

        val suoritemerkinnat =
            suoritemerkintaRepository.findForSeurantajakso(
                userId,
                seurantajakso.alkamispaiva!!,
                seurantajakso.paattymispaiva!!
            )
        suoritemerkinnat.forEach { it.lukittu = true }
        suoritemerkintaRepository.saveAll(suoritemerkinnat)

        val koulutusjaksot = seurantajakso.koulutusjaksot
        if (koulutusjaksot != null && koulutusjaksot.size > 0) {
            koulutusjaksot.forEach { it.lukittu = true }
            koulutusjaksoRepository.saveAll(koulutusjaksot)
        }

        mailService.sendEmailFromTemplate(
            kayttajaRepository.findById(seurantajakso.kouluttaja?.id!!).get().user!!,
            "uusiSeurantajakso.html",
            "email.uusiseurantajakso.title",
            properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
        )

        return seurantajaksoMapper.toDto(seurantajakso)
    }

    override fun update(
        seurantajaksoDTO: SeurantajaksoDTO,
        userId: String
    ): SeurantajaksoDTO {

        var seurantajakso =
            seurantajaksoRepository.findById(seurantajaksoDTO.id!!)
                .orElseThrow { EntityNotFoundException("Seurantajaksoa ei löydy") }

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)

        val updatedSeurantajakso =
            seurantajaksoMapper.toEntity(seurantajaksoDTO)

        if (kirjautunutErikoistuvaLaakari != null
            && kirjautunutErikoistuvaLaakari == seurantajakso.erikoistuvaLaakari
        ) {
            if (seurantajakso.seurantakeskustelunYhteisetMerkinnat == null || seurantajakso.korjausehdotus != null) {
                seurantajakso.omaArviointi = updatedSeurantajakso.omaArviointi
                seurantajakso.lisahuomioita = updatedSeurantajakso.lisahuomioita
                seurantajakso.seuraavanJaksonTavoitteet =
                    updatedSeurantajakso.seuraavanJaksonTavoitteet
                seurantajakso.seurantakeskustelunYhteisetMerkinnat =
                    updatedSeurantajakso.seurantakeskustelunYhteisetMerkinnat
                seurantajakso.seuraavanKeskustelunAjankohta =
                    updatedSeurantajakso.seuraavanKeskustelunAjankohta
                seurantajakso.korjausehdotus = null
            }
            seurantajakso = seurantajaksoRepository.save(seurantajakso)

            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(seurantajakso.kouluttaja?.id!!).get().user!!,
                "seurantajaksonYhteisetMerkinnat.html",
                "email.seurantajaksonyhteisetmerkinnat.title",
                properties = mapOf(Pair(MailProperty.ID, seurantajakso.id!!.toString()))
            )
        }

        if (seurantajakso.kouluttaja?.user?.id == userId) {
            seurantajakso.edistyminenTavoitteidenMukaista =
                updatedSeurantajakso.edistyminenTavoitteidenMukaista
            seurantajakso.huolenaiheet = updatedSeurantajakso.huolenaiheet
            seurantajakso.kouluttajanArvio = updatedSeurantajakso.kouluttajanArvio
            seurantajakso.erikoisalanTyoskentelyvalmiudet =
                updatedSeurantajakso.erikoisalanTyoskentelyvalmiudet
            seurantajakso.jatkotoimetJaRaportointi = updatedSeurantajakso.jatkotoimetJaRaportointi
            seurantajakso = seurantajaksoRepository.save(seurantajakso)
        }

        return seurantajaksoMapper.toDto(seurantajakso)
    }

    @Transactional(readOnly = true)
    override fun findByErikoistuvaLaakariKayttajaUserId(userId: String): List<SeurantajaksoDTO> {
        return seurantajaksoRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
            .map(seurantajaksoMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(
        id: Long,
        userId: String
    ): SeurantajaksoDTO? {
        return seurantajaksoRepository
            .findByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)?.let {
                seurantajaksoMapper.toDto(it)
            }
    }

    override fun delete(id: Long, userId: String) {
        val seurantajakso =
            seurantajaksoRepository.findByIdAndErikoistuvaLaakariKayttajaUserId(id, userId)

        if (seurantajakso != null) {
            mailService.sendEmailFromTemplate(
                kayttajaRepository.findById(seurantajakso.kouluttaja?.id!!).get().user!!,
                "seurantajaksoPoistettu.html",
                "email.seurantajaksopoistettu.title",
                properties = mapOf(
                    Pair(
                        MailProperty.NAME,
                        seurantajakso.erikoistuvaLaakari?.kayttaja?.user?.getName().toString()
                    )
                )
            )

            // Poistetaan lukot jos ei päällekkäisiä seurantajaksoja
            val seurantajaksot =
                seurantajaksoRepository.findByErikoistuvaLaakariKayttajaUserId(userId)
                    .filter { it.id != seurantajakso.id }
            val arvioinnit =
                suoritusarviointiRepository.findForSeurantajakso(
                    userId,
                    seurantajakso.alkamispaiva!!,
                    seurantajakso.paattymispaiva!!
                )
            arvioinnit.forEach { arviointi ->
                if (!onkoSeurantajaksolla(seurantajaksot, arviointi.tapahtumanAjankohta!!)) {
                    arviointi.lukittu = false
                }
            }
            suoritusarviointiRepository.saveAll(arvioinnit)

            val suoritemerkinnat = suoritemerkintaRepository.findForSeurantajakso(
                userId, seurantajakso.alkamispaiva!!, seurantajakso.paattymispaiva!!
            )
            suoritemerkinnat.forEach { suoritemerkinta ->
                if (!onkoSeurantajaksolla(seurantajaksot, suoritemerkinta.suorituspaiva!!)) {
                    suoritemerkinta.lukittu = false
                }
            }
            suoritemerkintaRepository.saveAll(suoritemerkinnat)

            val koulutusjaksot = seurantajakso.koulutusjaksot
            if (koulutusjaksot != null && koulutusjaksot.size > 0) {
                koulutusjaksot.forEach { koulutusjakso ->
                    if (seurantajaksot.none { it.koulutusjaksot?.contains(koulutusjakso) == true }) {
                        koulutusjakso.lukittu = false
                    }
                }
                koulutusjaksoRepository.saveAll(koulutusjaksot)
            }

            seurantajaksoRepository.delete(seurantajakso)
        }
    }

    private fun onkoSeurantajaksolla(seurantajaksot: List<Seurantajakso>, pvm: LocalDate): Boolean {
        seurantajaksot.forEach {
            if (pvm.isAfter(it.alkamispaiva) && pvm.isBefore(
                    it.paattymispaiva
                )
            ) {
                return true
            }
        }
        return false
    }
}
