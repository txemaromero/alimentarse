package es.alimentarse.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import es.alimentarse.usuario.UsuarioMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/** El hash rehecho al entrar con un coste antiguo (D-090). */
@ExtendWith(MockitoExtension.class)
class UsuarioDetallesServiceTest {

    private static final String ANTIGUO = "$2a$10$antiguo";
    private static final String NUEVO = "$2a$12$nuevo";
    private static final UserDetails ANA =
            User.withUsername("ana").password(ANTIGUO).roles("USER").build();

    @Mock
    UsuarioMapper usuarios;

    @InjectMocks
    UsuarioDetallesService servicio;

    @Test
    void guardaElHashRehechoYConservaLosRoles() {
        when(usuarios.rehacerHashPassword("ana", ANTIGUO, NUEVO)).thenReturn(1);

        UserDetails actualizado = servicio.updatePassword(ANA, NUEVO);

        assertThat(actualizado.getPassword()).isEqualTo(NUEVO);
        assertThat(actualizado.getAuthorities()).isEqualTo(ANA.getAuthorities());
    }

    @Test
    void siLaContrasenaHaCambiadoMientrasTantoNoLaPisa() {
        when(usuarios.rehacerHashPassword("ana", ANTIGUO, NUEVO)).thenReturn(0);

        assertThat(servicio.updatePassword(ANA, NUEVO)).isSameAs(ANA);
    }

    @Test
    void siNoSePuedeGuardarElLoginSigue() {
        when(usuarios.rehacerHashPassword("ana", ANTIGUO, NUEVO)).thenThrow(new QueryTimeoutException("bloqueo"));

        assertThat(servicio.updatePassword(ANA, NUEVO)).isSameAs(ANA);
    }
}
