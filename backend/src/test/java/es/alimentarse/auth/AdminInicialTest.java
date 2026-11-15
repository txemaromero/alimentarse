package es.alimentarse.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.alimentarse.support.PropiedadesDePrueba;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminInicialTest {

    @Mock
    UsuarioMapper usuarios;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void creaElAdministradorConRolesUserYAdmin() {
        new AdminInicial(
                        usuarios,
                        passwordEncoder,
                        PropiedadesDePrueba.conAdminInicial("clave-admin", "admin@toca.local"))
                .run(null);

        ArgumentCaptor<Usuario> creado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).insertar(creado.capture());
        assertThat(creado.getValue().id()).isEqualTo("admin");
        assertThat(creado.getValue().email()).isEqualTo("admin@toca.local");
        assertThat(passwordEncoder.matches("clave-admin", creado.getValue().password()))
                .isTrue();
        verify(usuarios).asignarRol("admin", "USER");
        verify(usuarios).asignarRol("admin", "ADMIN");
    }

    @Test
    void siYaExisteNoLoTocaAunqueCambieLaVariable() {
        when(usuarios.existeId("admin")).thenReturn(true);

        new AdminInicial(
                        usuarios,
                        passwordEncoder,
                        PropiedadesDePrueba.conAdminInicial("otra-clave", "admin@toca.local"))
                .run(null);

        verify(usuarios, never()).insertar(any());
    }

    @Test
    void siAdminSeDioDeBajaPeroHayOtrosAdministradoresNoReaparece() {
        when(usuarios.existeId("admin")).thenReturn(false);
        when(usuarios.hayAdministradores()).thenReturn(true);

        new AdminInicial(
                        usuarios,
                        passwordEncoder,
                        PropiedadesDePrueba.conAdminInicial("clave-admin", "admin@toca.local"))
                .run(null);

        verify(usuarios, never()).insertar(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "corta"})
    void sinContrasenaValidaNoLoCrea(String password) {
        new AdminInicial(usuarios, passwordEncoder, PropiedadesDePrueba.conAdminInicial(password, "admin@toca.local"))
                .run(null);

        verify(usuarios, never()).insertar(any());
    }

    @Test
    void siElEmailYaLoUsaOtraCuentaNoLoCrea() {
        when(usuarios.existeEmail("lucia@toca.local", null)).thenReturn(true);

        new AdminInicial(
                        usuarios,
                        passwordEncoder,
                        PropiedadesDePrueba.conAdminInicial("clave-admin", "lucia@toca.local"))
                .run(null);

        verify(usuarios, never()).insertar(any());
    }

    /** 40 «ñ» son 80 bytes: bcrypt no la cifraría y el arranque fallaría (D-090). */
    @Test
    void siLaContrasenaNoCabeEnBcryptNoLoCrea() {
        new AdminInicial(
                        usuarios,
                        passwordEncoder,
                        PropiedadesDePrueba.conAdminInicial("ñ".repeat(40), "admin@toca.local"))
                .run(null);

        verify(usuarios, never()).insertar(any());
    }
}
