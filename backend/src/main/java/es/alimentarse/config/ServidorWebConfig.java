package es.alimentarse.config;

import java.util.Map;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ajustes del servidor web incrustado. */
@Configuration(proxyBeanMethods = false)
public class ServidorWebConfig {

    /** Tipo del manifest de la PWA (9). Spring Boot no lo trae en su tabla y lo serviría como binario. */
    static final Map<String, String> TIPOS_PWA = Map.of("webmanifest", "application/manifest+json");

    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> tiposMimePwa() {
        return fabrica -> fabrica.addMimeMappings(new MimeMappings(TIPOS_PWA));
    }
}
