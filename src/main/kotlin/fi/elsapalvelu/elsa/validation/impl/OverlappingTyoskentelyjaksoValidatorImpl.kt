package fi.elsapalvelu.elsa.validation.impl

import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi
import fi.elsapalvelu.elsa.extensions.isInRange
import fi.elsapalvelu.elsa.repository.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.TyoskentelyjaksonPituusCounterService
import fi.elsapalvelu.elsa.service.constants.hyvaksiluettavatDays
import fi.elsapalvelu.elsa.service.dto.HyvaksiluettavatCounterData
import fi.elsapalvelu.elsa.service.dto.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.validation.OverlappingTyoskentelyjaksoValidator
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max

private const val ENTITY_NAME_TYOSKENTELYJAKSO = "tyoskentelyjakso"
private const val ENTITY_NAME_KESKEYTYSAIKA = "keskeytysaika"

@Service
class OverlappingTyoskentelyjaksoValidatorImpl(
    private val tyoskentelyjaksoRepository: TyoskentelyjaksoRepository,
    private val keskeytysaikaRepository: KeskeytysaikaRepository,
    private val tyoskentelyjaksonPituusCounterService: TyoskentelyjaksonPituusCounterService
) : OverlappingTyoskentelyjaksoValidator {

    override fun validateTyoskentelyjakso(
        userId: String,
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO
    ): Boolean {
        val tyoskentelyjaksoEndDate =
            tyoskentelyjaksoDTO.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByErikoistuvaUntilDateEagerWithRelationships(
                userId,
                tyoskentelyjaksoEndDate
            )

        if (tyoskentelyjaksot.isEmpty()) return true

        updateExistingTyoskentelyjaksoIfExists(tyoskentelyjaksoDTO, tyoskentelyjaksot)

        return validateTyoskentelyaika(
            tyoskentelyjaksoDTO.id,
            tyoskentelyjaksoDTO.alkamispaiva!!,
            tyoskentelyjaksoEndDate,
            tyoskentelyjaksot,
            tyoskentelyjaksoDTO.osaaikaprosentti!!.toDouble()
        )
    }

    override fun validateKeskeytysaika(
        userId: String,
        keskeytysaikaDTO: KeskeytysaikaDTO
    ): Boolean {
        val tyoskentelyjaksoEndDate =
            keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())

        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByErikoistuvaUntilDateEagerWithRelationships(
                userId,
                tyoskentelyjaksoEndDate
            )

        if (tyoskentelyjaksot.size == 1) return true

        updateExistingTyoskentelyjaksoKeskeytysaika(keskeytysaikaDTO, tyoskentelyjaksot)

        return validateTyoskentelyaika(
            keskeytysaikaDTO.tyoskentelyjaksoId,
            keskeytysaikaDTO.tyoskentelyjakso?.alkamispaiva!!,
            keskeytysaikaDTO.tyoskentelyjakso?.paattymispaiva!!,
            tyoskentelyjaksot
        )
    }

    override fun validateKeskeytysaikaDelete(
        userId: String,
        keskeytysaikaId: Long
    ): Boolean {
        val keskeytysaika =
            keskeytysaikaRepository.findOneByIdAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
                keskeytysaikaId,
                userId
            ) ?: throw BadRequestAlertException(
                "Keskeytysaikaa ei löydy",
                ENTITY_NAME_KESKEYTYSAIKA,
                "dataillegal"
            )

        val tyoskentelyjaksoId = keskeytysaika.tyoskentelyjakso?.id!!
        val tyoskentelyjaksoEndDate =
            keskeytysaika.tyoskentelyjakso?.paattymispaiva ?: LocalDate.now(ZoneId.systemDefault())
        val tyoskentelyjaksot =
            tyoskentelyjaksoRepository.findAllByErikoistuvaUntilDateEagerWithRelationships(
                userId,
                tyoskentelyjaksoEndDate
            )

        if (tyoskentelyjaksot.size == 1) return true

        removeKeskeytysaikaFromTyoskententelyjakso(keskeytysaika.id!!, tyoskentelyjaksoId, tyoskentelyjaksot)

        return validateTyoskentelyaika(
            tyoskentelyjaksoId,
            keskeytysaika.tyoskentelyjakso?.alkamispaiva!!,
            keskeytysaika.tyoskentelyjakso?.paattymispaiva!!,
            tyoskentelyjaksot
        )
    }

    private fun validateTyoskentelyaika(
        existingTyoskentelyjaksoId: Long?,
        tyoskentelyjaksoStartDate: LocalDate,
        tyoskentelyjaksoEndDate: LocalDate,
        tyoskentelyjaksot: List<Tyoskentelyjakso>,
        osaaikaProsentti: Double? = null
    ): Boolean {
        var hyvaksiluettavatCounterData: HyvaksiluettavatCounterData? = null
        fun getHyvaksiluettavatCounterData(
            tyoskentelyjaksot: List<Tyoskentelyjakso>,
            calculateUntilDate: LocalDate
        ): HyvaksiluettavatCounterData {
            if (hyvaksiluettavatCounterData == null) {
                hyvaksiluettavatCounterData =
                    tyoskentelyjaksonPituusCounterService.calculateHyvaksiluettavatDaysLeft(
                        tyoskentelyjaksot,
                        calculateUntilDate
                    )
            }
            return hyvaksiluettavatCounterData as HyvaksiluettavatCounterData
        }

        dates@ for (date in tyoskentelyjaksoStartDate.datesUntil(tyoskentelyjaksoEndDate.plusDays(1))) {
            val overlappingTyoskentelyjaksotForCurrentDate =
                tyoskentelyjaksot.filter {
                    date.isInRange(it.alkamispaiva!!, it.paattymispaiva)
                }
            if (overlappingTyoskentelyjaksotForCurrentDate.isEmpty()) continue@dates

            var overallTyoskentelyaikaFactorForCurrentDate = overlappingTyoskentelyjaksotForCurrentDate.sumOf {
                it.osaaikaprosentti!!.toDouble() / 100.0
            }

            // Jos kyseessä uusi työskentelyjakso, lisätään työskentelyaika päiväkohtaiseen kertymään.
            if (existingTyoskentelyjaksoId == null && osaaikaProsentti != null)
                overallTyoskentelyaikaFactorForCurrentDate += osaaikaProsentti / 100.0

            // Työaika ei ylitä 100% kyseisen päivän osalta -> ei tarvetta tarkastella poissaoloja.
            if (overallTyoskentelyaikaFactorForCurrentDate <= 1) continue@dates

            tyoskentelyjaksot@ for (tyoskentelyjakso in overlappingTyoskentelyjaksotForCurrentDate) {
                val keskeytyksetForCurrentDate = tyoskentelyjakso.keskeytykset.filter { keskeytysaika ->
                    date.isInRange(keskeytysaika.alkamispaiva!!, keskeytysaika.paattymispaiva)
                }

                keskeytyksetForCurrentDate.forEach {
                    val keskeytysaikaFactor =
                        it.osaaikaprosentti!!.toDouble() / 100.0 * (tyoskentelyjakso.osaaikaprosentti!!.toDouble() / 100.0)

                    when (it.poissaolonSyy?.vahennystyyppi) {
                        PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN -> {
                            overallTyoskentelyaikaFactorForCurrentDate -= keskeytysaikaFactor
                        }
                        PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA -> {
                            // Lasketaan hyväksiluetut päivät vain kerran ja vain ensimmäistä yli 100% allokaation
                            // ylittävää päivää edeltävään päivään saakka.
                            val counterData =
                                getHyvaksiluettavatCounterData(tyoskentelyjaksot, date.minusDays(1))
                            val reducedFactor = counterData.hyvaksiluettavatDays - keskeytysaikaFactor
                            // Jos reducedFactor on negatiivinen, ei hyväksiluettavia päiviä ole enää jäljellä, joten
                            // vähennetään työskentelyajasta ei-hyvitettävä osa.
                            if (reducedFactor < 0) overallTyoskentelyaikaFactorForCurrentDate -= abs(reducedFactor)
                            counterData.hyvaksiluettavatDays = max(0.0, reducedFactor)
                        }
                        PoissaolonSyyTyyppi.VAHENNETAAN_YLIMENEVA_AIKA_PER_VUOSI -> {
                            val counterData =
                                getHyvaksiluettavatCounterData(tyoskentelyjaksot, date.minusDays(1))
                            if (!counterData.hyvaksiluettavatPerYearMap.keys.contains(date.year)) {
                                counterData.hyvaksiluettavatPerYearMap[date.year] = hyvaksiluettavatDays
                            }
                            val reducedFactor =
                                counterData.hyvaksiluettavatPerYearMap[date.year]!! - keskeytysaikaFactor
                            if (reducedFactor < 0) overallTyoskentelyaikaFactorForCurrentDate -= abs(reducedFactor)
                            counterData.hyvaksiluettavatPerYearMap[date.year] = max(0.0, reducedFactor)
                        }
                    }
                }
            }

            if (overallTyoskentelyaikaFactorForCurrentDate > 1) {
                return false
            }
        }
        return true
    }

    private fun updateExistingTyoskentelyjaksoIfExists(
        tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ) {
        // Jos ollaan päivittämässä olemassaolevaa jaksoa, haetaan työskentelyjakso id:n perusteella ja päivitetään
        // tiedot suoraan siihen.
        if (tyoskentelyjaksoDTO.id != null) {
            tyoskentelyjaksot.find { it.id == tyoskentelyjaksoDTO.id }?.apply {
                if (this.hasTapahtumia()) {
                    paattymispaiva = tyoskentelyjaksoDTO.paattymispaiva
                } else {
                    alkamispaiva = tyoskentelyjaksoDTO.alkamispaiva
                    paattymispaiva = tyoskentelyjaksoDTO.paattymispaiva
                    osaaikaprosentti = tyoskentelyjaksoDTO.osaaikaprosentti
                }
            } ?: throw BadRequestAlertException(
                "Työskentelyjaksoa ei löydy",
                ENTITY_NAME_TYOSKENTELYJAKSO,
                "dataillegal"
            )
        }
    }

    private fun updateExistingTyoskentelyjaksoKeskeytysaika(
        keskeytysaikaDTO: KeskeytysaikaDTO,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ): Tyoskentelyjakso {
        val tyoskentelyjaksoWithUpdatedKeskeytysaika =
            findTyoskentelyjakso(keskeytysaikaDTO.tyoskentelyjaksoId!!, tyoskentelyjaksot)

        tyoskentelyjaksoWithUpdatedKeskeytysaika.keskeytykset.find {
            it.id == keskeytysaikaDTO.id
        }?.apply {
            alkamispaiva = keskeytysaikaDTO.alkamispaiva
            paattymispaiva = keskeytysaikaDTO.paattymispaiva
            osaaikaprosentti = keskeytysaikaDTO.osaaikaprosentti
            poissaolonSyy?.vahennystyyppi = keskeytysaikaDTO.poissaolonSyy?.vahennystyyppi
        } ?: throw BadRequestAlertException(
            "Keskeytysaikaa ei löydy",
            ENTITY_NAME_KESKEYTYSAIKA,
            "dataillegal"
        )

        return tyoskentelyjaksoWithUpdatedKeskeytysaika
    }

    private fun removeKeskeytysaikaFromTyoskententelyjakso(
        keskeytysaikaId: Long,
        tyoskentelyjaksoId: Long,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ) {
        val tyoskentelyjaksoWithRemovedKeskeytysaika = findTyoskentelyjakso(tyoskentelyjaksoId, tyoskentelyjaksot)
        tyoskentelyjaksoWithRemovedKeskeytysaika.keskeytykset.removeIf {
            it.id == keskeytysaikaId
        }
    }

    private fun findTyoskentelyjakso(
        id: Long,
        tyoskentelyjaksot: List<Tyoskentelyjakso>
    ): Tyoskentelyjakso {
        return tyoskentelyjaksot.find {
            it.id == id
        } ?: throw BadRequestAlertException(
            "Työskentelyjaksoa, johon keskeytysaika kohdistuu ei löydy",
            ENTITY_NAME_TYOSKENTELYJAKSO,
            "dataillegal"
        )
    }
}
