package fi.elsapalvelu.elsa.service.mapper

import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoDTO
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.mapstruct.ReportingPolicy

@Mapper(componentModel = "spring",
    uses = [
        OpintooikeusMapper::class,
        ErikoistuvaLaakariMapper::class,
        KayttajaMapper:: class,
        UserMapper::class
    ], unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface ValmistumispyyntoMapper : EntityMapper<ValmistumispyyntoDTO, Valmistumispyynto> {

    @Mappings(
        Mapping(source = "opintooikeus.erikoistuvaLaakari.kayttaja.nimi", target = "erikoistujanNimi"),
        Mapping(source = "opintooikeus.erikoistuvaLaakari.kayttaja.user.avatar", target = "erikoistuvanAvatar"),
        Mapping(source = "opintooikeus.opiskelijatunnus", target = "erikoistujanOpiskelijatunnus"),
        Mapping(source = "opintooikeus.erikoistuvaLaakari.syntymaaika", target = "erikoistujanSyntymaaika"),
        Mapping(source = "opintooikeus.yliopisto.nimi", target = "erikoistujanYliopisto"),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaiva",
            target = "erikoistujanLaillistamispaiva"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaivanLiitetiedosto",
            target = "erikoistujanLaillistamistodistus"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi",
            target = "erikoistujanLaillistamistodistusNimi"
        ),
        Mapping(
            source = "opintooikeus.erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi",
            target = "erikoistujanLaillistamistodistusTyyppi"
        ),
        Mapping(source = "opintooikeus.asetus.nimi", target = "asetus"),
        Mapping(source = "opintooikeus.opintooikeudenMyontamispaiva", target = "opintooikeudenMyontamispaiva"),
        Mapping(target = "virkailijanSaate", ignore = true)
    )
    override fun toDto(entity: Valmistumispyynto): ValmistumispyyntoDTO
}
