package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.domain.User
import io.github.jhipster.config.JHipsterProperties
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.nio.charset.StandardCharsets
import java.util.*
import javax.mail.MessagingException

enum class MailProperty(val property: String) {
    USER("user"),
    BASE_URL("baseUrl"),
    ID("id"),
    NAME("name"),
    TEXT("text"),
    DATE("date")
}

@Service
class MailService(
    private val jHipsterProperties: JHipsterProperties,
    private val javaMailSender: JavaMailSender,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    ) {
        log.debug(
            "Send email[multipart '$isMultipart' and html '$isHtml']" +
                " to '$to' with subject '$subject' and content=$content"
        )

        val mimeMessage = javaMailSender.createMimeMessage()
        try {
            MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name()).apply {
                setTo(to)
                setFrom(jHipsterProperties.mail.from)
                setSubject(subject)
                setText(content, isHtml)
            }
            javaMailSender.send(mimeMessage)
            log.debug("Sent email to User '$to'")
        } catch (ex: MailException) {
            log.warn("Email could not be sent to user '$to': ${ex.message}")
        } catch (ex: MessagingException) {
            log.warn("Email could not be sent to user '$to': ${ex.message}")
        }
    }

    @Async
    fun sendEmailFromTemplate(
        user: User,
        templateName: String,
        titleKey: String,
        properties: Map<MailProperty, String>
    ) {
        if (user.email == null) {
            log.debug("Email doesn't exist for user '${user.login}'")
            return
        }
        var locale = Locale.forLanguageTag("fi")
        if (user.langKey != null) locale = Locale.forLanguageTag(user.langKey)

        val context = Context(locale).apply {
            setVariable(MailProperty.USER.property, user)
            setVariable(MailProperty.BASE_URL.property, jHipsterProperties.mail.baseUrl)
            properties.forEach {
                setVariable(it.key.property, it.value)
            }
        }
        val content = templateEngine.process("mail/${templateName}", context)
        val subject = messageSource.getMessage(titleKey, null, locale)
        sendEmail(user.email!!, subject, content, isMultipart = false, isHtml = true)
    }
}
