package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.service.ArviointityokaluService
import fi.elsapalvelu.elsa.service.dto.ArviointityokaluKategoriaDTO
import fi.elsapalvelu.elsa.service.mapper.ArviointityokaluKategoriaService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/tekninen-paakayttaja")
class TekninenPaakayttajaArviointityokalutResource(
    private val arviointityokaluService: ArviointityokaluService,
    private val arviointityokaluKategoriaService: ArviointityokaluKategoriaService
) {

    @GetMapping("/arviointityokalut/kategoriat")
    fun getErikoisalat(): ResponseEntity<List<ArviointityokaluKategoriaDTO>> {
        return ResponseEntity.ok(arviointityokaluKategoriaService.findAll())
    }

    @PostMapping("/arviointityokalut/kategoria")
    fun createArviointityokaluKategoria(
        @Valid @RequestBody arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO
    ): ResponseEntity<ArviointityokaluKategoriaDTO> {
        return ResponseEntity.ok(arviointityokaluKategoriaService.create(arviointityokaluKategoriaDTO))
    }

    // @PutMapping("/arviointityokalut/kategoria")
    // fun updateArviointityokaluKategoria(
    //     @Valid @RequestBody arviointityokaluKategoriaDTO: ArviointityokaluKategoriaDTO
    // ): ResponseEntity<ArviointityokaluKategoriaDTO> {
    //     return ResponseEntity.ok(arviointityokaluKategoriaService.update(arviointityokaluKategoriaDTO))
    // }

    @DeleteMapping("/arviointityokalut/kategoria/{id}")
    fun deleteArviointityokaluKategoria(
        @PathVariable id: Long,
        principal: Principal?
    ): ResponseEntity<Void> {
        arviointityokaluKategoriaService.delete(id)
        return ResponseEntity.noContent().build()
    }

}
