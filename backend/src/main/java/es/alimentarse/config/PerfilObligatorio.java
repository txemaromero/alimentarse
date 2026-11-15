package es.alimentarse.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Sin un perfil explícito, la aplicación no arranca (D-089). Antes, sin {@code SPRING_PROFILES_ACTIVE}, el perfil era
 * {@code dev}: un despliegue que olvidase la variable quedaba con las cuentas de demostración, Swagger y la cookie sin
 * {@code Secure}. Se comprueba al preparar el entorno, antes de crear nada (ni la conexión a la base de datos). Se
 * registra en {@code META-INF/spring.factories}; {@code mvnw spring-boot:run} usa {@code dev} (pom.xml).
 */
public class PerfilObligatorio implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    static final String SIN_PERFIL = "Falta el perfil: define la variable de entorno SPRING_PROFILES_ACTIVE"
            + " (prod en producción, dev en desarrollo). Sin ella, la aplicación no arranca (D-089).";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evento) {
        exigirPerfil(evento.getEnvironment());
    }

    static void exigirPerfil(Environment entorno) {
        if (entorno.getActiveProfiles().length == 0) {
            throw new IllegalStateException(SIN_PERFIL);
        }
    }
}
