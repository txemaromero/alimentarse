package es.alimentarse.support;

import es.alimentarse.seguridad.JwtService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/** Beans de apoyo de los tests de integración: buzón de correo y avisos push en memoria, y tokens de prueba. */
@TestConfiguration(proxyBeanMethods = false)
public class ApoyoTestConfig {

    /** Sustituye al {@code JavaMailSender} real: ningún test envía correos. */
    @Bean
    BuzonDePrueba buzonDePrueba() {
        return new BuzonDePrueba();
    }

    /** Sustituye al envío push real: ningún test envía avisos a un servicio push (D-078). */
    @Bean
    @Primary
    EnvioPushDePrueba envioPushDePrueba() {
        return new EnvioPushDePrueba();
    }

    @Bean
    TokensDePrueba tokensDePrueba(JwtService jwt) {
        return new TokensDePrueba(jwt);
    }

    @Bean
    DatosDePrueba datosDePrueba(JdbcTemplate jdbc) {
        return new DatosDePrueba(jdbc);
    }
}
