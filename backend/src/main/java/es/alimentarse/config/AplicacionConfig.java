package es.alimentarse.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Beans transversales. {@code @EnableAsync}: los correos se envían en segundo plano (D-020). {@code @EnableScheduling}:
 * los recordatorios de las comidas planificadas (D-078).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AplicacionConfig {

    /** Reloj de la aplicación; los tests lo sustituyen por uno fijo para probar caducidades y ventanas de tiempo. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
