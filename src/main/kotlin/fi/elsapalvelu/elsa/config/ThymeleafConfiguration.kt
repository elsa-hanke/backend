package fi.elsapalvelu.elsa.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

import org.thymeleaf.templateresolver.ITemplateResolver


@Configuration
class ThymeleafConfiguration {

    @Bean
    fun thymeleafTemplateResolver(): ITemplateResolver? {
        val templateResolver = ClassLoaderTemplateResolver()
        templateResolver.prefix = "templates/"
        templateResolver.suffix = ".html"
        templateResolver.setTemplateMode("HTML")
        templateResolver.characterEncoding = "UTF-8"
        return templateResolver
    }
}
