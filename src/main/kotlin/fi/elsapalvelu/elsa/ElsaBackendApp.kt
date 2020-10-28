package fi.elsapalvelu.elsa

import fi.elsapalvelu.elsa.config.ApplicationProperties
import io.github.jhipster.config.DefaultProfileUtil
import io.github.jhipster.config.JHipsterConstants
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.core.env.Environment
import java.net.InetAddress
import java.net.UnknownHostException
import javax.annotation.PostConstruct

@SpringBootApplication
@EnableConfigurationProperties(LiquibaseProperties::class, ApplicationProperties::class)
class ElsaBackendApp(private val env: Environment) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initApplication() {
        val activeProfiles = env.activeProfiles
        if (
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)
        ) {
            log.error(
                "You have misconfigured your application!" +
                    "It should not run with both the 'dev' and 'prod' profiles at the same time."
            )
        }
        if (
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_CLOUD)
        ) {
            log.error(
                "You have misconfigured your application!" +
                    "It should not run with both the 'dev' and 'cloud' profiles at the same time."
            )
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val env = runApplication<ElsaBackendApp>(*args) { DefaultProfileUtil.addDefaultProfile(this) }.environment
            logApplicationStartup(env)
        }

        @JvmStatic
        private fun logApplicationStartup(env: Environment) {
            val log = LoggerFactory.getLogger(ElsaBackendApp::class.java)

            val protocol = if (env.getProperty("server.ssl.key-store") != null) {
                "https"
            } else "http"
            val serverPort = env.getProperty("server.port")
            val contextPath = env.getProperty("server.servlet.context-path") ?: "/"
            var hostAddress = "localhost"
            try {
                hostAddress = InetAddress.getLocalHost().hostAddress
            } catch (e: UnknownHostException) {
                log.warn("The host name could not be determined, using `localhost` as fallback")
            }
            log.info(
                """

                ----------------------------------------------------------
                Application '${env.getProperty("spring.application.name")}' is running! Access URLs:
                Local:      $protocol://localhost:$serverPort$contextPath
                External:   $protocol://$hostAddress:$serverPort$contextPath
                Profile(s): ${env.activeProfiles.joinToString(",")}
                ----------------------------------------------------------
                """.trimIndent()
            )
        }
    }
}
