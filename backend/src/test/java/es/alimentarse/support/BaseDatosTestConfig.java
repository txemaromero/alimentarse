package es.alimentarse.support;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

/** Base de datos de los tests de integración (D-016). */
@TestConfiguration(proxyBeanMethods = false)
public class BaseDatosTestConfig {

    /** Con Docker, un MySQL efímero sustituye a la conexión de {@code TEST_DB_URL}. */
    @Bean
    @ServiceConnection
    @Conditional(DockerDisponible.class)
    MySQLContainer mysql() {
        return new MySQLContainer("mysql:8.4").withDatabaseName("toca_test");
    }

    /** Cada contexto de test empieza con la base de datos vacía y recién migrada. */
    @Bean
    FlywayMigrationStrategy limpiarYMigrar() {
        return flyway -> {
            exigirBaseDeDatosDeTest(flyway.getConfiguration().getDataSource());
            flyway.clean();
            flyway.migrate();
        };
    }

    /**
     * Impide borrar por error una base de datos que no sea de test (por ejemplo, la de desarrollo si
     * {@code TEST_DB_URL} apuntase a ella): su nombre debe terminar en {@code _test}.
     */
    public static void exigirBaseDeDatosDeTest(DataSource dataSource) {
        try (Connection conexion = dataSource.getConnection()) {
            String nombre = conexion.getCatalog();
            if (nombre == null || !nombre.endsWith("_test")) {
                throw new IllegalStateException("Los tests solo pueden borrar una base de datos cuyo nombre termine en"
                        + " _test, y la configurada es '" + nombre + "'. Revisa TEST_DB_URL.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo conectar con la base de datos de test", e);
        }
    }

    static class DockerDisponible implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return DockerClientFactory.instance().isDockerAvailable();
        }
    }
}
