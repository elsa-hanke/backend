package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityManager

private const val ERIKOISTUJIEN_SEURANTA_ENDPOINT_URL: String =
    "/api/virkailija/etusivu/erikoistujien-seuranta"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VirkailijaEtusivuResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restEtusivuMockMvc: MockMvc

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    private lateinit var virkailija: Kayttaja

    private lateinit var defaultYliopisto: Yliopisto

    private lateinit var anotherYliopisto: Yliopisto

    private lateinit var erikoistuvaLaakari1: ErikoistuvaLaakari

    private lateinit var erikoistuvaLaakari2: ErikoistuvaLaakari

    private lateinit var erikoisala1: Erikoisala

    private lateinit var erikoisala2: Erikoisala

    private lateinit var asetus1: Asetus

    private lateinit var asetus2: Asetus

    @BeforeEach
    fun setup() {
        defaultYliopisto = Yliopisto(nimi = defaultYliopistoEnum)
        em.persist(defaultYliopisto)

        anotherYliopisto = Yliopisto(nimi = anotherYliopistoEnum)
        em.persist(anotherYliopisto)

        erikoisala1 = ErikoisalaHelper.createEntity(nimi = erikoisala1Nimi)
        em.persist(erikoisala1)

        erikoisala2 = ErikoisalaHelper.createEntity(nimi = erikoisala2Nimi)
        em.persist(erikoisala2)

        asetus1 = Asetus(nimi = asetus1Nimi)
        em.persist(asetus1)

        asetus2 = Asetus(nimi = asetus2Nimi)
        em.persist(asetus2)

        val opintoopas =
            OpintoopasHelper.createEntity(em, LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(20L), erikoisala1)
        em.persist(opintoopas)

        erikoistuvaLaakari1 =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                yliopisto = defaultYliopisto,
                erikoisala = erikoisala1,
                asetus = asetus1
            ).apply {
                kayttaja?.user?.firstName = "John"
                kayttaja?.user?.lastName = "Doe"
            }
        em.persist(erikoistuvaLaakari1)

        erikoistuvaLaakari2 = ErikoistuvaLaakariHelper.createEntity(
            em,
            yliopisto = defaultYliopisto,
            erikoisala = erikoisala2,
            asetus = asetus2
        ).apply {
            kayttaja?.user?.firstName = "John Don"
            kayttaja?.user?.lastName = "Doe"
        }
        em.persist(erikoistuvaLaakari2)
    }

    @Test
    @Transactional
    fun shouldListErikoistuvatUnderSameYliopisto() {
        initTest()

        erikoistuvaLaakari2.getOpintooikeusKaytossa()?.yliopisto = anotherYliopisto

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                erikoistuvaLaakari1.kayttaja?.user
            )
        )

        em.persist(
            TeoriakoulutusHelper.createEntity(
                em,
                erikoistuvaLaakari1.kayttaja?.user,
                LocalDate.ofEpochDay(0),
                LocalDate.ofEpochDay(5),
                37.5
            )
        )

        em.persist(
            TeoriakoulutusHelper.createEntity(
                em,
                erikoistuvaLaakari1.kayttaja?.user,
                LocalDate.ofEpochDay(5),
                LocalDate.ofEpochDay(10),
                20.0

            )
        )

        val johtamisopinnotTyyppi = OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO)
        em.persist(johtamisopinnotTyyppi)

        val sateilysuojakoulutusTyyppi = OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS)
        em.persist(sateilysuojakoulutusTyyppi)

        val kuulusteluTyyppi = OpintosuoritusTyyppi(nimi = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU)
        em.persist(kuulusteluTyyppi)

        em.persist(
            Opintosuoritus(
                nimi_fi = "Johtamisopinto 1",
                kurssikoodi = "JOHT-1",
                suorituspaiva = LocalDate.ofEpochDay(3L),
                opintopisteet = 3.0,
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari1.getOpintooikeusKaytossa(),
                tyyppi = johtamisopinnotTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Johtamisopinto 2",
                kurssikoodi = "JOHT-2",
                suorituspaiva = LocalDate.ofEpochDay(4L),
                opintopisteet = 2.0,
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari1.getOpintooikeusKaytossa(),
                tyyppi = johtamisopinnotTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Säteilysuojakoulutus 1",
                kurssikoodi = "SÄT-1",
                suorituspaiva = LocalDate.ofEpochDay(3L),
                opintopisteet = 1.0,
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari1.getOpintooikeusKaytossa(),
                tyyppi = sateilysuojakoulutusTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Säteilysuojakoulutus 2",
                kurssikoodi = "SÄT-2",
                suorituspaiva = LocalDate.ofEpochDay(4L),
                opintopisteet = 1.0,
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari1.getOpintooikeusKaytossa(),
                tyyppi = sateilysuojakoulutusTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Kuulustelu 1",
                kurssikoodi = "TENTTI-1",
                suorituspaiva = LocalDate.ofEpochDay(3L),
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari1.getOpintooikeusKaytossa(),
                tyyppi = kuulusteluTyyppi
            )
        )

        em.persist(
            Opintosuoritus(
                nimi_fi = "Kuulustelu 2",
                kurssikoodi = "TENTTI-2",
                suorituspaiva = LocalDate.ofEpochDay(4L),
                hyvaksytty = true,
                opintooikeus = erikoistuvaLaakari1.getOpintooikeusKaytossa(),
                tyyppi = kuulusteluTyyppi
            )
        )

        restEtusivuMockMvc.perform(get(ERIKOISTUJIEN_SEURANTA_ENDPOINT_URL))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariId").value(
                    erikoistuvaLaakari1.id
                )
            )
            .andExpect(jsonPath("$.content[0].etunimi").value(erikoistuvaLaakari1.kayttaja?.user?.firstName))
            .andExpect(jsonPath("$.content[0].sukunimi").value(erikoistuvaLaakari1.kayttaja?.user?.lastName))
            .andExpect(jsonPath("$.content[0].syntymaaika").value(erikoistuvaLaakari1.syntymaaika.toString()))
            .andExpect(jsonPath("$.content[0].erikoisala").value(erikoistuvaLaakari1.getErikoisalaNimi()))
            .andExpect(
                jsonPath("$.content[0].asetus").value(
                    erikoistuvaLaakari1.getOpintooikeusKaytossa()?.asetus?.nimi
                )
            )
            .andExpect(jsonPath("$.content[0].erikoisala").value(erikoistuvaLaakari1.getErikoisalaNimi()))
            .andExpect(jsonPath("$.content[0].koejaksoTila").value(KoejaksoTila.EI_AKTIIVINEN.toString()))
            .andExpect(
                jsonPath("$.content[0].opintooikeudenMyontamispaiva").value(
                    erikoistuvaLaakari1.getOpintooikeusKaytossa()?.opintooikeudenMyontamispaiva.toString()
                )
            )
            .andExpect(
                jsonPath("$.content[0].opintooikeudenPaattymispaiva").value(
                    erikoistuvaLaakari1.getOpintooikeusKaytossa()?.opintooikeudenPaattymispaiva.toString()
                )
            )
            .andExpect(
                jsonPath("$.content[0].tyoskentelyjaksoTilastot.tyoskentelyaikaYhteensa").value(
                    5
                )
            )
            .andExpect(jsonPath("$.content[0].teoriakoulutuksetSuoritettu").value(57.5))
            .andExpect(
                jsonPath("$.content[0].teoriakoulutuksetVaadittu").value(
                    OpintoopasHelper.DEFAULT_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA
                )
            )
            .andExpect(jsonPath("$.content[0].johtamisopinnotSuoritettu").value(5.0))
            .andExpect(
                jsonPath("$.content[0].johtamisopinnotVaadittu").value(
                    OpintoopasHelper.DEFAULT_ERIKOISALAN_VAATIMA_JOHTAMISOPINTOJEN_VAHIMMAISMAARA
                )
            )
            .andExpect(jsonPath("$.content[0].sateilysuojakoulutuksetSuoritettu").value(2.0))
            .andExpect(
                jsonPath("$.content[0].sateilysuojakoulutuksetVaadittu").value(
                    OpintoopasHelper.DEFAULT_ERIKOISALAN_VAATIMA_SATEILYSUOJAKOULUTUSTEN_VAHIMMAISMAARA
                )
            )
            .andExpect(jsonPath("$.content[0].valtakunnallisetKuulustelutSuoritettuLkm").value(2))
    }

    @Test
    @Transactional
    fun filterByNameShouldMatchIntoFirstErikoistuva() {
        initTest()

        val query = "?nimi.contains=john doe"

        restEtusivuMockMvc.perform(get(ERIKOISTUJIEN_SEURANTA_ENDPOINT_URL + query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariId").value(
                    erikoistuvaLaakari1.id
                )
            )
    }

    @Test
    @Transactional
    fun filterByNameShouldMatchIntoSecondErikoistuva() {
        initTest()

        val query = "?nimi.contains=don"

        restEtusivuMockMvc.perform(get(ERIKOISTUJIEN_SEURANTA_ENDPOINT_URL + query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariId").value(
                    erikoistuvaLaakari2.id
                )
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["joh", "john do", "doe john", "doe", "doe jo", "john"])
    @Transactional
    fun filterByNameShouldMatchIntoBothErikoistuvat(input: String) {
        initTest()

        val query = "?nimi.contains=$input"

        restEtusivuMockMvc.perform(get(ERIKOISTUJIEN_SEURANTA_ENDPOINT_URL + query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(2)))
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariId").value(
                    erikoistuvaLaakari1.id
                )
            )
            .andExpect(
                jsonPath("$.content[1].erikoistuvaLaakariId").value(
                    erikoistuvaLaakari2.id
                )
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["john doe d", "j d", "j doe", "doe john j", "d j", "d john"])
    @Transactional
    fun filterByNameShouldNotMatchIntoAnyErikoistuva(input: String) {
        initTest()

        val query = "?nimi.contains=$input"

        restEtusivuMockMvc.perform(get(ERIKOISTUJIEN_SEURANTA_ENDPOINT_URL + query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(0)))
    }

    @Test
    @Transactional
    fun filterByErikoisalaShouldMatchIntoFirstErikoistuva() {
        initTest()

        val query = "?erikoisalaId.equals=${erikoisala1.id}"

        restEtusivuMockMvc.perform(get(ERIKOISTUJIEN_SEURANTA_ENDPOINT_URL + query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariId").value(
                    erikoistuvaLaakari1.id
                )
            )
    }

    @Test
    @Transactional
    fun filterByAsetusShouldMatchIntoFirstErikoistuva() {
        initTest()

        val query = "?asetusId.equals=${asetus1.id}"

        restEtusivuMockMvc.perform(get(ERIKOISTUJIEN_SEURANTA_ENDPOINT_URL + query))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(hasSize<Int>(1)))
            .andExpect(
                jsonPath("$.content[0].erikoistuvaLaakariId").value(
                    erikoistuvaLaakari1.id
                )
            )
    }

    @Test
    @Transactional
    fun getErikoistujienSeurantaRajaimet() {
        initTest()
        val erikoisalatCountByLiittynytElsaan = erikoisalaRepository.findAllByLiittynytElsaanTrue().count()

        restEtusivuMockMvc.perform(get("/api/virkailija/etusivu/erikoistujien-seuranta-rajaimet"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalat").value(hasSize<Int>(erikoisalatCountByLiittynytElsaan)))
            .andExpect(jsonPath("$.asetukset").value(hasSize<Int>(7)))
    }

    fun initTest() {
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(OPINTOHALLINNON_VIRKAILIJA))
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(OPINTOHALLINNON_VIRKAILIJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        virkailija = KayttajaHelper.createEntity(em, user)
        em.persist(virkailija)

        virkailija.yliopistot.add(defaultYliopisto)
    }

    companion object {
        private val defaultYliopistoEnum = YliopistoEnum.HELSINGIN_YLIOPISTO
        private val anotherYliopistoEnum = YliopistoEnum.OULUN_YLIOPISTO
        private val erikoisala1Nimi = "erikoisala1"
        private val erikoisala2Nimi = "erikoisala2"
        private val asetus1Nimi = "asetus1"
        private val asetus2Nimi = "asetus2"
    }
}
