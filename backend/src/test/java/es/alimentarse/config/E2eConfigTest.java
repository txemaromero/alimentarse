package es.alimentarse.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.junit.jupiter.api.Test;

/** El perfil e2e solo vacía bases de datos de pruebas (D-089). */
class E2eConfigTest {

    @Test
    void soloSonDePruebasLasQueTerminanEnTestOE2e() {
        assertThat(E2eConfig.esDePruebas("toca_test")).isTrue();
        assertThat(E2eConfig.esDePruebas("toca_e2e")).isTrue();
        assertThat(E2eConfig.esDePruebas("toca")).isFalse();
        assertThat(E2eConfig.esDePruebas("test_toca")).isFalse();
        assertThat(E2eConfig.esDePruebas(null)).isFalse();
    }

    @Test
    void noVaciaUnaBaseDeDatosQueNoEsDePruebas() throws SQLException {
        Flyway flyway = flywayContra(conCatalogo("toca"));

        assertThatThrownBy(() -> new E2eConfig().vaciarYMigrar().migrate(flyway))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("la configurada es 'toca'");
        verify(flyway, never()).clean();
        verify(flyway, never()).migrate();
    }

    @Test
    void vaciaYMigraLaDeLaIntegracionContinua() throws SQLException {
        Flyway flyway = flywayContra(conCatalogo("toca_e2e"));

        new E2eConfig().vaciarYMigrar().migrate(flyway);

        verify(flyway).clean();
        verify(flyway).migrate();
    }

    @Test
    void siNoPuedeComprobarLaBaseDeDatosNoArranca() throws SQLException {
        DataSource sinConexion = mock(DataSource.class);
        when(sinConexion.getConnection()).thenThrow(new SQLException("Sin conexión"));

        assertThatThrownBy(() -> E2eConfig.exigirBaseDeDatosDePruebas(sinConexion))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Sin conexión");
        assertThatCode(() -> E2eConfig.exigirBaseDeDatosDePruebas(conCatalogo("toca_test")))
                .doesNotThrowAnyException();
    }

    private static DataSource conCatalogo(String nombre) throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection conexion = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conexion);
        when(conexion.getCatalog()).thenReturn(nombre);
        return dataSource;
    }

    private static Flyway flywayContra(DataSource dataSource) {
        Flyway flyway = mock(Flyway.class);
        Configuration configuracion = mock(Configuration.class);
        when(flyway.getConfiguration()).thenReturn(configuracion);
        when(configuracion.getDataSource()).thenReturn(dataSource);
        return flyway;
    }
}
