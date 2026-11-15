package es.alimentarse.correo;

import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Motor Thymeleaf solo para las plantillas de correo de {@code src/main/resources/mail/}. Evalúa las expresiones con
 * SpEL ({@link SpringTemplateEngine}), sin OGNL (D-086).
 */
@Configuration
class CorreoConfig {

    @Bean
    TemplateEngine plantillasCorreo() {
        ClassLoaderTemplateResolver resolutor = new ClassLoaderTemplateResolver();
        resolutor.setPrefix("mail/");
        resolutor.setSuffix(".html");
        resolutor.setTemplateMode(TemplateMode.HTML);
        resolutor.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolutor.setCacheable(true);
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolutor);
        return motor;
    }
}
