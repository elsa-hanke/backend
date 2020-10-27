package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiCriteria
import fi.elsapalvelu.elsa.service.dto.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.mapper.SuoritusarviointiMapper
import io.github.jhipster.service.QueryService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.criteria.Join

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@Service
@Transactional(readOnly = true)
class SuoritusarviointiQueryService(
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val suoritusarviointiMapper: SuoritusarviointiMapper
) : QueryService<Suoritusarviointi>() {

    @Transactional(readOnly = true)
    fun findByCriteria(criteria: SuoritusarviointiCriteria?): MutableList<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria)
        return suoritusarviointiMapper.toDto(suoritusarviointiRepository.findAll(specification))
    }

    @Transactional(readOnly = true)
    fun findByCriteria(criteria: SuoritusarviointiCriteria?, page: Pageable): Page<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria)
        return suoritusarviointiRepository.findAll(specification, page)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun findByCriteriaAndTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(
        criteria: SuoritusarviointiCriteria?,
        userId: String,
        page: Pageable

    ): Page<SuoritusarviointiDTO> {
        val specification = createSpecification(criteria) { root, _, cb ->
            val user: Join<Kayttaja, User> = root.join(Suoritusarviointi_.tyoskentelyjakso)
                .join(Tyoskentelyjakso_.erikoistuvaLaakari)
                .join(ErikoistuvaLaakari_.kayttaja)
                .join(Kayttaja_.user)
            cb.equal(user.get(User_.id), userId)
        }

        return suoritusarviointiRepository.findAll(specification, page)
            .map(suoritusarviointiMapper::toDto)
    }

    @Transactional(readOnly = true)
    fun countByCriteria(criteria: SuoritusarviointiCriteria?): Long {
        val specification = createSpecification(criteria)
        return suoritusarviointiRepository.count(specification)
    }

    protected fun createSpecification(
        criteria: SuoritusarviointiCriteria?,
        spec: Specification<Suoritusarviointi?>? = null
    ): Specification<Suoritusarviointi?> {
        var specification: Specification<Suoritusarviointi?> = Specification.where(spec)
        if (criteria != null) {
            if (criteria.id != null) {
                specification = specification.and(buildRangeSpecification(criteria.id, Suoritusarviointi_.id))
            }
            if (criteria.tapahtumanAjankohta != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.tapahtumanAjankohta, Suoritusarviointi_.tapahtumanAjankohta))
            }
            if (criteria.arvioitavaTapahtuma != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.arvioitavaTapahtuma, Suoritusarviointi_.arvioitavaTapahtuma))
            }
            if (criteria.pyynnonAika != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.pyynnonAika, Suoritusarviointi_.pyynnonAika))
            }
            if (criteria.vaativuustaso != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.vaativuustaso, Suoritusarviointi_.vaativuustaso))
            }
            if (criteria.sanallinenArvio != null) {
                specification = specification
                    .and(buildStringSpecification(criteria.sanallinenArvio, Suoritusarviointi_.sanallinenArvio))
            }
            if (criteria.arviointiAika != null) {
                specification = specification
                    .and(buildRangeSpecification(criteria.arviointiAika, Suoritusarviointi_.arviointiAika))
            }
        }
        return specification
    }
}
