package es.alimentarse.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class BaseDatosTestConfigTest {

    @Test
    void permiteBorrarUnaBaseDeDatosDeTest() throws SQLException {
        assertThatCode(() -> BaseDatosTestConfig.exigirBaseDeDatosDeTest(conCatalogo("toca_test")))
                .doesNotThrowAnyException();
    }

    @Test
    void seNiegaABorrarLaBaseDeDatosDeDesarrollo() throws SQLException {
        assertThatThrownBy(() -> BaseDatosTestConfig.exigirBaseDeDatosDeTest(conCatalogo("toca")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'toca'");
    }

    private static DataSource conCatalogo(String catalogo) throws SQLException {
        Connection conexion = mock(Connection.class);
        when(conexion.getCatalog()).thenReturn(catalogo);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(conexion);
        return dataSource;
    }
}
