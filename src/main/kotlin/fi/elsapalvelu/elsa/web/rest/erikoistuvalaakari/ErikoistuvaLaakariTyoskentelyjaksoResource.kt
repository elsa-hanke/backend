package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import com.fasterxml.jackson.databind.ObjectMapper
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
    private val overlappingKeskeytysaikaValidationService: OverlappingKeskeytysaikaValidationService
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
            validateNewTyoskentelyjaksoDTO(it)
            validatePaattymispaiva(user.id!!, it)
            validateTyoskentelyaika(user.id!!, it)

            val asiakirjaDTOs = getMappedFiles(files, user.id!!) ?: mutableSetOf()
            tyoskentelyjaksoService.create(it, user.id!!, asiakirjaDTOs)?.let { result ->
                return ResponseEntity
                    .created(URI("/api/tyoskentelyjaksot/${result.id}"))
                    .body(result)
            }

        } ?: throw BadRequestAlertException(
            "Työskentelyjakson lisääminen epäonnistui.",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.tyoskentelyjakson-lisaaminen-epaonnistui"
        )
    }

    @PutMapping("/tyoskentelyjaksot")
    fun updateTyoskentelyjakso(
        @Valid @RequestParam tyoskentelyjaksoJson: String,
        @Valid @RequestParam files: List<MultipartFile>?,
        @RequestParam deletedAsiakirjaIdsJson: String?,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
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
            validatePaattymispaiva(user.id!!, it)
            validateTyoskentelyaika(user.id!!, it)

            val newAsiakirjat = getMappedFiles(files, user.id!!) ?: mutableSetOf()
            val deletedAsiakirjaIds = deletedAsiakirjaIdsJson?.let { id ->
                objectMapper.readValue(id, mutableSetOf<Int>()::class.java)
            }
            tyoskentelyjaksoService.update(it, user.id!!, newAsiakirjat, deletedAsiakirjaIds)
                ?.let { result ->
                    return ResponseEntity.ok(result)
                }
        } ?: throw BadRequestAlertException(
            "Työskentelyjakson päivittäminen epäonnistui.",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.tyoskentelyjakson-paivittaminen-epaonnistui"
        )
    }

    @GetMapping("/tyoskentelyjaksot-taulukko")
    fun getTyoskentelyjaksoTable(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksotTableDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val table = TyoskentelyjaksotTableDTO()
        table.poissaolonSyyt = poissaolonSyyService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        table.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        table.keskeytykset = keskeytysaikaService
            .findAllByTyoskentelyjaksoErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()
        table.tilastot = tyoskentelyjaksoService.getTilastot(user.id!!)

        return ResponseEntity.ok(table)
    }

    @GetMapping("/tyoskentelyjaksot")
    fun getTyoskentelyjaksot(
        principal: Principal?
    ): ResponseEntity<List<TyoskentelyjaksoDTO>> {
        val user = userService.getAuthenticatedUser(principal)
        val tyoskentelyjaksot =
            tyoskentelyjaksoService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!)

        return ResponseEntity.ok(tyoskentelyjaksot)
    }

    @GetMapping("/tyoskentelyjaksot/{id}")
    fun getTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoDTO> {
        val user = userService.getAuthenticatedUser(principal)
        tyoskentelyjaksoService.findOne(id, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/{id}")
    fun deleteTyoskentelyjakso(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)

        asiakirjaService.removeTyoskentelyjaksoReference(user.id!!, id)
        if (!tyoskentelyjaksoService.delete(id, user.id!!)) {
            throw BadRequestAlertException(
                "Työskentelyjakson poistaminen epäonnistui",
                TYOSKENTELYJAKSO_ENTITY_NAME,
                "dataillegal.tyoskentelyjakson-poistaminen-epaonnistui"
            )
        }
        return ResponseEntity
            .noContent()
            .build()
    }

    @GetMapping("/tyoskentelyjakso-lomake")
    fun getTyoskentelyjaksoForm(
        principal: Principal?
    ): ResponseEntity<TyoskentelyjaksoFormDTO> {
        val user = userService.getAuthenticatedUser(principal)
        val form = TyoskentelyjaksoFormDTO()

        form.kunnat = kuntaService.findAll().toMutableSet()

        form.erikoisalat = erikoisalaService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        form.reservedAsiakirjaNimet =
            asiakirjaService.findAllByErikoistuvaLaakariUserId(user.id!!).map { it.nimi!! }
                .toMutableSet()

        return ResponseEntity.ok(form)
    }

    @GetMapping("/poissaolo-lomake")
    fun getKeskeytysaikaForm(
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaFormDTO> {
        val user = userService.getAuthenticatedUser(principal)

        val form = KeskeytysaikaFormDTO()

        form.poissaolonSyyt = poissaolonSyyService.findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

        form.tyoskentelyjaksot = tyoskentelyjaksoService
            .findAllByErikoistuvaLaakariKayttajaUserId(user.id!!).toMutableSet()

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

        if (!overlappingKeskeytysaikaValidationService.validateKeskeytysaika(user.id!!, keskeytysaikaDTO)) {
            throw BadRequestAlertException(
                "Päällekkäisten poissaolojen päiväkohtainen kertymä ei voi ylittää 100%:a",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.paallekkaisten-poissaolojen-yhteenlaskettu-aika-ylittyy"
            )
        }

        keskeytysaikaService.save(keskeytysaikaDTO, user.id!!)?.let {
            return ResponseEntity
                .created(URI("/api/tyoskentelyjaksot/poissaolot/${it.id}"))
                .body(it)
        } ?: throw BadRequestAlertException(
            "Keskeytysajan lisääminen epäonnistui",
            KESKEYTYSAIKA_ENTITY_NAME,
            "dataillegal.keskeytysajan-lisaaminen-epaonnistui"
        )
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

        if (!overlappingKeskeytysaikaValidationService.validateKeskeytysaika(user.id!!, keskeytysaikaDTO)) {
            throw BadRequestAlertException(
                "Päällekkäisten poissaolojen päiväkohtainen kertymä ei voi ylittää 100%:a",
                KESKEYTYSAIKA_ENTITY_NAME,
                "dataillegal.paallekkaisten-poissaolojen-yhteenlaskettu-aika-ylittyy"
            )
        }

        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaika(user.id!!, keskeytysaikaDTO)
        ) {
            throwOverlappingTyoskentelyjaksotException()
        }

        keskeytysaikaService.save(keskeytysaikaDTO, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw BadRequestAlertException(
            "Keskeytysajan päivittäminen epäonnistui",
            KESKEYTYSAIKA_ENTITY_NAME,
            "dataillegal.keskeytysajan-paivittaminen-epaonnistui"
        )
    }

    @GetMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun getKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<KeskeytysaikaDTO> {
        val user = userService.getAuthenticatedUser(principal)
        keskeytysaikaService.findOne(id, user.id!!)?.let {
            return ResponseEntity.ok(it)
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @DeleteMapping("/tyoskentelyjaksot/poissaolot/{id}")
    fun deleteKeskeytysaika(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        val user = userService.getAuthenticatedUser(principal)

        if (!overlappingTyoskentelyjaksoValidationService.validateKeskeytysaikaDelete(user.id!!, id)) {
            throwOverlappingTyoskentelyjaksotException()
        }

        keskeytysaikaService.delete(id, user.id!!)
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
            user.id!!,
            tyoskentelyjaksoDTO.liitettyKoejaksoon!!
        )?.let {
            val response = ResponseEntity.ok()
            return if (tyoskentelyjaksoDTO.liitettyKoejaksoon!!) response.body(it) else response.build()
        } ?: throw BadRequestAlertException(
            "Työskentelyjakson päivittäminen epäonnistui.",
            TYOSKENTELYJAKSO_ENTITY_NAME,
            "dataillegal.tyoskentelyjakson-paivittaminen-epaonnistui"
        )
    }

    private fun getMappedFiles(
        files: List<MultipartFile>?,
        userId: String
    ): MutableSet<AsiakirjaDTO>? {
        files?.let {
            fileValidationService.validate(it, userId)

            if (!fileValidationService.validate(it, userId)) {
                throw BadRequestAlertException(
                    "Tiedosto ei ole kelvollinen tai samanniminen tiedosto on jo olemassa.",
                    ASIAKIRJA_ENTITY_NAME,
                    "dataillegal.tiedosto-ei-ole-kelvollinen-tai-samanniminen-tiedosto-on-jo-olemassa"
                )
            }

            return it.map { file ->
                AsiakirjaDTO(
                    nimi = file.originalFilename,
                    tyyppi = file.contentType,
                    asiakirjaData = AsiakirjaDataDTO(
                        fileInputStream = file.inputStream,
                        fileSize = file.size
                    )
                )
            }.toMutableSet()
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

    private fun validateTyoskentelyaika(userId: String, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        if (!overlappingTyoskentelyjaksoValidationService.validateTyoskentelyjakso(userId, tyoskentelyjaksoDTO)) {
            throwOverlappingTyoskentelyjaksotException()
        }
    }

    private fun validatePaattymispaiva(userId: String, tyoskentelyjaksoDTO: TyoskentelyjaksoDTO) {
        tyoskentelyjaksoDTO.paattymispaiva?.isBefore(tyoskentelyjaksoDTO.alkamispaiva)?.let {
            if (it) {
                throw BadRequestAlertException(
                    "Työskentelyjakson päättymispäivä ei saa olla ennen alkamisaikaa",
                    TYOSKENTELYPAIKKA_ENTITY_NAME,
                    "dataillegal.tyoskentelyjakson-paattymispaiva-ei-saa-olla-ennen-alkamisaikaa"
                )
            }
        }

        if (!tyoskentelyjaksoService.validatePaattymispaiva(tyoskentelyjaksoDTO, userId)) {
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
