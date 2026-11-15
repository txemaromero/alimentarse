package es.alimentarse.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.TestIntegracion;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TestIntegracion
@Transactional
class UsuarioMapperTest {

    @Autowired
    UsuarioMapper usuarios;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void crearUsuario() {
        usuarios.insertar(Usuario.nuevo("mapper", "Ana", "López", null, "ana@pruebas.local", "hash"));
        usuarios.asignarRol("mapper", "USER");
        usuarios.asignarRol("mapper", "ADMIN");
    }

    @Test
    void laCaducidadDelTokenSeGuardaEnUtcYSeLeeComoElMismoInstante() {
        Instant expiracion = Instant.parse("2026-11-04T10:15:30Z");

        usuarios.guardarTokenPassword("mapper", "hash-token", expiracion);

        assertThat(usuarios.buscarPorTokenPassword("hash-token"))
                .hasValueSatisfying(u -> assertThat(u.tokenExpiracion()).isEqualTo(expiracion));
        assertThat(jdbc.queryForObject(
                        "SELECT DATE_FORMAT(TOKEN_EXPIRACION, '%Y-%m-%d %H:%i:%s') FROM usuarios WHERE ID = 'mapper'",
                        String.class))
                .isEqualTo("2026-11-04 10:15:30");
    }

    @Test
    void cambiarLaContrasenaAnulaElTokenDeRecuperacion() {
        usuarios.guardarTokenPassword("mapper", "hash-token", Instant.parse("2026-11-04T10:15:30Z"));

        usuarios.actualizarPassword("mapper", "nuevo-hash");

        assertThat(usuarios.buscarPorTokenPassword("hash-token")).isEmpty();
        assertThat(usuarios.buscarPorId("mapper")).hasValueSatisfying(u -> {
            assertThat(u.password()).isEqualTo("nuevo-hash");
            assertThat(u.tokenExpiracion()).isNull();
        });
    }

    /** La misma contraseña con otro coste (D-090): ni cierra las sesiones ni anula el enlace de recuperación. */
    @Test
    void rehacerElHashSoloCambiaLaContrasenaYSoloSiSigueSiendoLaMisma() {
        usuarios.guardarTokenPassword("mapper", "hash-token", Instant.parse("2026-11-04T10:15:30Z"));
        Integer version = versionSesion();

        assertThat(usuarios.rehacerHashPassword("mapper", "HASH", "nuevo-hash"))
                .as("Un hash distingue mayúsculas, aunque la colación de la tabla no")
                .isZero();
        assertThat(usuarios.rehacerHashPassword("mapper", "hash", "nuevo-hash")).isOne();

        assertThat(usuarios.buscarPorId("mapper"))
                .hasValueSatisfying(u -> assertThat(u.password()).isEqualTo("nuevo-hash"));
        assertThat(usuarios.buscarPorTokenPassword("hash-token")).isPresent();
        assertThat(versionSesion()).isEqualTo(version);
    }

    private Integer versionSesion() {
        return jdbc.queryForObject("SELECT VERSION_SESION FROM usuarios WHERE ID = 'mapper'", Integer.class);
    }

    @Test
    void lasBusquedasPorIdYEmailNoDistinguenMayusculas() {
        assertThat(usuarios.buscarPorEmail("ANA@Pruebas.local"))
                .hasValueSatisfying(u -> assertThat(u.id()).isEqualTo("mapper"));
        assertThat(usuarios.existeId("MAPPER")).isTrue();
    }

    @Test
    void existeEmailPuedeExcluirAlPropioUsuario() {
        assertThat(usuarios.existeEmail("ana@pruebas.local", null)).isTrue();
        assertThat(usuarios.existeEmail("ana@pruebas.local", "mapper")).isFalse();
    }

    @Test
    void losRolesSeDevuelvenEnOrden() {
        assertThat(usuarios.rolesDe("mapper")).containsExactly("USER", "ADMIN");
    }
}
