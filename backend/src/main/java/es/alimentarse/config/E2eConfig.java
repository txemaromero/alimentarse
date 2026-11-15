package es.alimentarse.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Perfil {@code e2e}: el que usan las pruebas de extremo a extremo (Playwright). Al arrancar vacía la base de datos y
 * la vuelve a crear con las migraciones, para que cada ejecución empiece igual. Nunca se activa en otro perfil y,
 * además, exige {@code spring.flyway.clean-disabled: false}, que solo pone {@code application-e2e.yml}, y una base de
 * datos de pruebas (D-089).
 */
@Configuration(proxyBeanMethods = false)
@Profile("e2e")
public class E2eConfig {

    @Bean
    FlywayMigrationStrategy vaciarYMigrar() {
        return flyway -> {
            exigirBaseDeDatosDePruebas(flyway.getConfiguration().getDataSource());
            flyway.clean();
            flyway.migrate();
        };
    }

    /**
     * El perfil va dentro del JAR: si alguien lo activase por error contra la base de datos de desarrollo o de
     * producción, la vaciaría. Solo se aceptan las de pruebas, cuyo nombre termina en {@code _test} (la de los tests
     * del backend, que es la que usa en local) o {@code _e2e} (la de la integración continua).
     */
    static void exigirBaseDeDatosDePruebas(DataSource dataSource) {
        String nombre;
        try (Connection conexion = dataSource.getConnection()) {
            nombre = conexion.getCatalog();
        } catch (SQLException e) {
            throw new IllegalStateException("No se ha podido comprobar la base de datos del perfil e2e", e);
        }
        if (!esDePruebas(nombre)) {
            throw new IllegalStateException("El perfil e2e vacía la base de datos al arrancar y solo admite una cuyo"
                    + " nombre termine en _test o _e2e, pero la configurada es '" + nombre + "'.");
        }
    }

    static boolean esDePruebas(String nombre) {
        return nombre != null && (nombre.endsWith("_test") || nombre.endsWith("_e2e"));
    }
}
