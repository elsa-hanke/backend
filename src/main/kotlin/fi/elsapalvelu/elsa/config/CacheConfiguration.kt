package fi.elsapalvelu.elsa.config

import io.github.jhipster.config.JHipsterProperties
import io.github.jhipster.config.cache.PrefixedKeyGenerator
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.jsr107.Eh107Configuration
import org.hibernate.cache.jcache.ConfigSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration(
    @Autowired val gitProperties: GitProperties?,
    @Autowired val buildProperties: BuildProperties?,
    jHipsterProperties: JHipsterProperties
) {

    private val jcacheConfiguration: javax.cache.configuration.Configuration<Any, Any>

    init {
        val ehcache = jHipsterProperties.cache.ehcache

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Any::class.java,
                Any::class.java,
                ResourcePoolsBuilder.heap(ehcache.maxEntries)
            )
                .withExpiry(
                    ExpiryPolicyBuilder
                        .timeToLiveExpiration(Duration.ofSeconds(ehcache.timeToLiveSeconds.toLong()))
                )
                .build()
        )
    }

    @Bean
    fun hibernatePropertiesCustomizer(
        cacheManager: javax.cache.CacheManager
    ) = HibernatePropertiesCustomizer { hibernateProperties ->
        hibernateProperties[ConfigSettings.CACHE_MANAGER] = cacheManager
    }

    @Bean
    @Suppress("MaxLineLength")
    fun cacheManagerCustomizer(): JCacheManagerCustomizer {
        // @formatter:off
        return JCacheManagerCustomizer { cm ->
            createCache(cm, fi.elsapalvelu.elsa.domain.Arviointiasteikko::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Arviointiasteikko::class.java.name + ".erikoisalat")
            createCache(cm, fi.elsapalvelu.elsa.domain.Arviointiasteikko::class.java.name + ".tasot")
            createCache(cm, fi.elsapalvelu.elsa.domain.ArviointiasteikonTaso::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Arviointityokalu::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.ArvioitavaOsaalue::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Asiakirja::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Authority::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.ArvioitavaKokonaisuus::class.java.name + ".arvioitavatOsaalueet")
            createCache(cm, fi.elsapalvelu.elsa.domain.ArvioitavaKokonaisuus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria::class.java.name + ".arvioitavatKokonaisuudet")
            createCache(cm, fi.elsapalvelu.elsa.domain.ArvioitavanKokonaisuudenKategoria::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Erikoisala::class.java.name + ".arvioitavatKokonaisuudet")
            createCache(cm, fi.elsapalvelu.elsa.domain.Erikoisala::class.java.name + ".kategoriat")
            createCache(cm, fi.elsapalvelu.elsa.domain.Erikoisala::class.java.name + ".yliopistot")
            createCache(cm, fi.elsapalvelu.elsa.domain.Erikoisala::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari::class.java.name + ".annetutValtuutukset")
            createCache(cm, fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari::class.java.name + ".tyoskentelyjaksot")
            createCache(cm, fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Kayttaja::class.java.name + ".saadutValtuutukset")
            createCache(cm, fi.elsapalvelu.elsa.domain.Kayttaja::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Keskeytysaika::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.KoejaksonAloituskeskustelu::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.KoejaksonKehittamistoimenpiteet::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.KoejaksonKoulutussopimus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.KoejaksonLoppukeskustelu::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.KoejaksonValiarviointi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.KoejaksonVastuuhenkilonArvio::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Kouluttajavaltuutus::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.KoulutussopimuksenKouluttaja::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.KoulutussopimuksenKoulutuspaikka::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Kunta::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Oppimistavoite::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.OppimistavoitteenKategoria::class.java.name + ".oppimistavoitteet")
            createCache(cm, fi.elsapalvelu.elsa.domain.OppimistavoitteenKategoria::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.OsaalueenArviointi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.PoissaolonSyy::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Suoritemerkinta::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.SuoritusarvioinninKommentti::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Suoritusarviointi::class.java.name + ".osaalueenArvioinnit")
            createCache(cm, fi.elsapalvelu.elsa.domain.Suoritusarviointi::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Tyoskentelyjakso::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Tyoskentelypaikka::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.User::class.java.name + ".authorities")
            createCache(cm, fi.elsapalvelu.elsa.domain.User::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.VerificationToken::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.domain.Yliopisto::class.java.name + ".erikoisalat")
            createCache(cm, fi.elsapalvelu.elsa.domain.Yliopisto::class.java.name + ".kayttajat")
            createCache(cm, fi.elsapalvelu.elsa.domain.Yliopisto::class.java.name)
            createCache(cm, fi.elsapalvelu.elsa.repository.UserRepository.USERS_BY_EMAIL_CACHE)
            createCache(cm, fi.elsapalvelu.elsa.repository.UserRepository.USERS_BY_LOGIN_CACHE)
        }
        // @formatter:on
    }

    private fun createCache(cm: javax.cache.CacheManager, cacheName: String) {
        val cache: javax.cache.Cache<Any, Any>? = cm.getCache(cacheName)
        if (cache == null) {
            cm.createCache(cacheName, jcacheConfiguration)
        }
    }

    @Bean
    fun keyGenerator() = PrefixedKeyGenerator(gitProperties, buildProperties)
}
