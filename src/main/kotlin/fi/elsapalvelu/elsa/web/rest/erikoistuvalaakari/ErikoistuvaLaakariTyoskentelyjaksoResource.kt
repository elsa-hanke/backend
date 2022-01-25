package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.extensions.mapAsiakirja
import fi.elsapalvelu.elsa.service.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.Principal
import javax.validation.Valid

private const val TYOSKENTELYJAKSO_ENTITY_NAME = "tyoskentelyjakso"
private const val KESKEYTYSAIKA_ENTITY_NAME = "keskeytysaika"
private const val ASIAKIRJA_ENTITY_NAME = "asiakirja"
private const val TYOSKENTELYPAIKKA_ENTITY_NAME = "tyoskentelypaikka"

@RestController
@RequestMapping("/api/erikoistuva-laakari")
class ErikoistuvaLaakariTyoskentelyjaksoResource(
    private val userService: UserService,
    private val tyoskentelyjaksoService: TyoskentelyjaksoService,
    private val kuntaService: KuntaService,
    private val erikoisalaService: ErikoisalaService,
    private val poissaolonSyyService: PoissaolonSyyService,
    private val keskeytysaikaService: KeskeytysaikaService,
    private val asiakirjaService: AsiakirjaService,
    private val objectMapper: ObjectMapper,
    private val fileValidationService: FileValidationService,
    private val overlappingTyoskentelyjaksoValidationService: OverlappingTyoskentelyjaksoValidationService,
    private val overlappingKeskeytysaikaValidationService: OverlappingKeskeytysaikaValidationService,
    private val opintooikeusService: OpintooikeusService
) {

    @Value("\${jhipster.clientApp.name}")
    private var applicationName: String? = null

    @PostMapping("/tyoskentelyjaksot")
    fun createTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.let {
            val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
            validateNewTyoskentelyjaksoDTO(it)
            validatePaattymispaiva(opintooikeusId, it)
            validateTyoskentelyaika(opintooikeusId, it)

            val asiakirjaDTOs = getMappedFiles(files, opintooikeusId) ?: mutableSetOf()
            tyoskentelyjaksoService.create(it, opintooikeusId, asiakirjaDTOs)?.let { result ->
                return ResponseEntity
                    .created(URI("/api/tyoskentelyjaksot/${result.id}"))
                    .body(result)
            }

        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/tyoskentelyjaksot")
    fun updateTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        tyoskentelyjaksoJson.let {
            objectMapper.readValue(it, TyoskentelyjaksoDTO::class.java)
        }?.let {
            if (it.id == null) {
                throw BadRequestAlertException(
                    "Työskentelyjakson ID puuttuu.",
                    TYOSKENTELYJAKSO_ENTITY_NAME,
                    "idnull"
                )
            }
            validatePaattymispaiva(opintooikeusId, it)
            validateTyoskentelyaika(opintooikeusId, it)

            val newAsiakirjat = getMappedFiles(files, opintooikeusId) ?: mutableSetOf()
            val deletedAsiakirjaIds = deletedAsiakirjaIdsJson?.let { id ->
                objectMapper.readValue(id, mutableSetOf<Int>()::class.java)
            }
            tyoskentelyjaksoService.update(it, opintooikeusId, newAsiakirjat, deletedAsiakirjaIds)
                ?.let { result ->
                    return ResponseEntity.ok(result)
                }
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/tyoskentelyjaksot-taulukko")
    fun getTyoskentelyjaksoTable(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksotTableDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val table = TyoskentelyjaksotTableDTO()
        table.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeusId).toMutableSet()
        table.keskeytykset = keskeytysaikaService
            .findAllByTyoskentelyjaksoOpintooikeusId(opintooikeusId).toMutableSet()
        table.tilastot = tyoskentelyjaksoService.getTilastot(opintooikeusId)

        return ResponseEntity.ok(table)
    }

    @GetMapping("/tyoskentelyjaksot")
    fun getTyoskentelyjaksot(
        principal: Principal?
    ): ResponseEntity<List<TyoskentelyjaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val tyoskentelyjaksot =
            tyoskentelyjaksoService.findAllByOpintooikeusId(opintooikeusId)

        return ResponseEntity.ok(tyoskentelyjaksot)
    }

    @GetMapping("/tyoskentelyjaksot/{id}")
    fun getTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        tyoskentelyjaksoService.findOne(id, opintooikeusId)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)

        asiakirjaService.removeTyoskentelyjaksoReference(opintooikeusId, id)
        if (tyoskentelyjaksoService.delete(id, opintooikeusId)) {
            return ResponseEntity
                .noContent()
                .build()

        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/tyoskentelyjakso-lomake")
    fun getTyoskentelyjaksoForm(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val form = TyoskentelyjaksoFormDTO()

        form.kunnat = kuntaService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAll().toMutableSet()

        form.reservedAsiakirjaNimet =
            asiakirjaService.findAllByOpintooikeusId(opintooikeusId).map { it.nimi!! }
                .toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/poissaolo-lomake")
    fun getKeskeytysaikaForm(
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        val form = KeskeytysaikaFormDTO()

        form.poissaolonSyyt = poissaolonSyyService.findAllByOpintooikeusId(opintooikeusId).toMutableSet()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByOpintooikeusId(opintooikeusId).toMutableSet()

        return ResponseEntity.ok(form)
    }

    @PostMapping("/tyoskentelyjaksot/poissaolot")
    fun createKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        if (keskeytysaikaDTO.id != null) {
            throw BadRequestAlertException(
                "Uusi keskeytysaika ei saa sisältää ID:tä",
                KESKEYTYSAIKA_ENTITY_NAME,
                "idexists"
            )
        }

        validateKeskeytysaikaDTO(keskeytysaikaDTO)
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!overlappingKeskeytysaikaValidationService.validateKeskeytysaika(opintooikeusId, keskeytysaikaDTO)) {
            throw BadRequestAlertException(
                "Päällekkäisten poissaolojen päiväkohtainen kertymä ei voi ylittää 100%:a",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.paallekkaisten-poissaolojen-yhteenlaskettu-aika-ylittyy"
            )
        }

        keskeytysaikaService.save(keskeytysaikaDTO, opintooikeusId)?.let {
            return ResponseEntity
                .created(URI("/api/tyoskentelyjaksot/poissaolot/${it.id}"))
                .body(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @PutMapping("/tyoskentelyjaksot/poissaolot")
    fun updateKeskeytysaika(
        @Valid @RequestBody keskeytysaikaDTO: KeskeytysaikaDTO,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        if (keskeytysaikaDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", TYOSKENTELYJAKSO_ENTITY_NAME, "idnull")
        }

        validateKeskeytysaikaDTO(keskeytysaikaDTO)
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!overlappingKeskeytysaikaValidationService.validateKeskeytysaika(opintooikeusId, keskeytysaikaDTO)) {
            throw BadRequestAlertException(
                "Päällekkäisten poissaolojen päiväkohtainen kertymä ei voi ylittää 100%:a",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.paallekkaisten-poissaolojen-yhteenlaskettu-aika-ylittyy"
            )
        }

        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaika(opintooikeusId, keskeytysaikaDTO)
        ) {
            throwOverlappingTyoskentelyjaksotException()
        }

        keskeytysaikaService.save(keskeytysaikaDTO, opintooikeusId)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    @GetMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun getKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        keskeytysaikaService.findOne(id, opintooikeusId)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun deleteKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaikaDelete(opintooikeusId, id)) {
            throwOverlappingTyoskentelyjaksotException()
        }

        keskeytysaikaService.delete(id, opintooikeusId)
        return ResponseEntity
            .noContent()
            .build()
    }

    @PatchMapping("/tyoskentelyjaksot/koejakso")
    fun updateLiitettyKoejaksoon(
        @RequestBody tyoskentelyjaksoDTO: TyoskentelyjaksoDTO,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO?> {
        val user = userService.getAuthenticatedUser(principal)
        val opintooikeusId = opintooikeusService.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(user.id!!)
        if (tyoskentelyjaksoDTO.id == null) {
            throw BadRequestAlertException("Virheellinen id", TYOSKENTELYJAKSO_ENTITY_NAME, "idnull")
        }

        if (tyoskentelyjaksoDTO.liitettyKoejaksoon == null) {
            throw BadRequestAlertException(
                "Liitetty koejaksoon on pakollinen tieto",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "dataillegal.liitetty-koejaksoon-on-pakollinen-tieto"
            )
        }

        tyoskentelyjaksoService.updateLiitettyKoejaksoon(
            tyoskentelyjaksoDTO.id!!,
            opintooikeusId,
            tyoskentelyjaksoDTO.liitettyKoejaksoon!!
        )?.let {
            val response = ResponseEntity.ok()
            return if (tyoskentelyjaksoDTO.liitettyKoejaksoon!!) response.body(it) else response.build()
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }

    private fun getMappedFiles(
        files: List<MultipartFile>?,
        opintooikeusId: Long
    ): MutableSet<AsiakirjaDTO>? {
        files?.let {
            if (!fileValidationService.validate(it, opintooikeusId)) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                    ASIAKIRJA_ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
                )
            }
            return it.map { file -> file.mapAsiakirja() }.toMutableSet()
        }

        return null
    }

    private fun validateNewTyoskentelyjaksoDTO(it: TyoskentelyjaksoDTO) {
        if (it.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelyjakso ei saa sisältää ID:tä",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "idexists"
            )
        }
        if (it.tyoskentelypaikka == null || it.tyoskentelypaikka!!.id != null) {
            throw BadRequestAlertException(
                "Uusi tyoskentelypaikka ei saa sisältää ID:tä",
                TYOSKENTELYPAIKKA_ENTITY_NAME,
                "idexists"
            )
        }
    }

    private fun validateTyoskentelyaika(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        if (!overlappingTyoskentelyjaksoValidationService.validateTyoskentelyjakso(
                opintooikeusId,
                tyoskentelyjaksoDTO
            )
        ) {
            throwOverlappingTyoskentelyjaksotException()
        }
    }

    private fun validatePaattymispaiva(opintooikeusId: Long, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        tyoskentelyjaksoDTO.paattymispaiva?.isBefore(tyoskentelyjaksoDTO.alkamispaiva)?.let {
            if (it) {
                throw BadRequestAlertException(
                    "Työskentelyjakson päättymispäivä ei saa olla ennen alkamisaikaa",
                    TYOSKENTELYPAIKKA_ENTITY_NAME,
                    "dataillegal.tyoskentelyjakson-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
                )
            }
        }

        if (!tyoskentelyjaksoService.validatePaattymispaiva(tyoskentelyjaksoDTO, opintooikeusId)) {
            throw BadRequestAlertException(
                "Työskentelyjakson päättymispäivä ei ole kelvollinen.",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "dataillegal.tyoskentelyjakson-paattymispaiva-ei-ole-kelvollinen"
            )
        }
    }

    private fun validateKeskeytysaikaDTO(keskeytysaikaDTO: KeskeytysaikaDTO) {
        if (keskeytysaikaDTO.alkamispaiva == null || keskeytysaikaDTO.paattymispaiva == null) {
            throw BadRequestAlertException(
                "Keskeytysajan alkamis- ja päättymispäivä ovat pakollisia tietoja",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysaika-alkamis-ja-paattymispaiva-ovat-pakollisia-tietoja"
            )
        }

        if (keskeytysaikaDTO.alkamispaiva!!.isAfter(keskeytysaikaDTO.paattymispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei saa olla ennen alkamisaikaa",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
            )
        }

        if (keskeytysaikaDTO.alkamispaiva!!.isBefore(keskeytysaikaDTO.tyoskentelyjakso!!.alkamispaiva)) {
            throw BadRequestAlertException(
                "Keskeytysajan alkamispäivä ei voi olla ennen työskentelyjakson alkamispäivää",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-alkamispaiva-ei-voi-olla-ennen-tyoskentelyjakson-alkamispaivaa"
            )
        }

        if (keskeytysaikaDTO.tyoskentelyjakso!!.paattymispaiva != null && keskeytysaikaDTO.paattymispaiva!!.isAfter(
                keskeytysaikaDTO.tyoskentelyjakso!!.paattymispaiva
            )
        ) {
            throw BadRequestAlertException(
                "Keskeytysajan päättymispäivä ei voi olla työskentelyjakson päättymispäivän jälkeen",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-paattymispaiva-ei-voi-olla-tyoskentelyjakson-paattymispaivan-jalkeen"
            )
        }

        if (keskeytysaikaDTO.tyoskentelyjakso == null) {
            throw BadRequestAlertException(
                "Keskeytysajan täytyy kohdistua työskentelyjaksoon",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.keskeytysajan-taytyy-kohdistua-tyoskentelyjaksoon"
            )
        }
    }

    private fun throwOverlappingTyoskentelyjaksotException() {
        throw BadRequestAlertException(
            "Päällekkäisten työskentelyjaksojen yhteenlaskettu työaika ei voi ylittää 100%:a",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.paallekkaisten-tyoskentelyjaksojen-yhteenlaskettu-aika-ylittyy"
        )
    }
}
