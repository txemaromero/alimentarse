package es.alimentarse.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.receta.RecetaService;
import es.alimentarse.receta.ValoracionService;
import es.alimentarse.usuario.UsuarioMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final UsuarioAdminDto LUCIA =
            new UsuarioAdminDto("lucia", "Lucía", "Fernández", "Ruiz", "lucia@toca.local", false, false);
    private static final UsuarioAdminDto LUCIA_ADMIN =
            new UsuarioAdminDto("lucia", "Lucía", "Fernández", "Ruiz", "lucia@toca.local", true, false);
    private static final UsuarioAdminDto LUCIA_BLOQUEADA =
            new UsuarioAdminDto("lucia", "Lucía", "Fernández", "Ruiz", "lucia@toca.local", false, true);
    private static final UsuarioAdminDto ADMIN =
            new UsuarioAdminDto("admin", "Administrador", "Toca", null, "a@b.c", true, false);

    @Mock
    AdminMapper admin;

    @Mock
    UsuarioMapper usuarios;

    @Mock
    RecetaService recetas;

    @Mock
    ValoracionService valoraciones;

    private AdminService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new AdminService(admin, usuarios, recetas, valoraciones);
    }

    @Test
    void listarBuscaConPatronYPagina() {
        when(admin.esAdmin("admin")).thenReturn(true);
        when(admin.contar("%luc%")).thenReturn(11L);
        when(admin.buscar("%luc%", 10, 10)).thenReturn(List.of(LUCIA));

        assertThat(servicio.listar(" luc ", "admin", 1, 10)).isEqualTo(new Pagina<>(List.of(LUCIA), 1, 10, 11, 2));
    }

    @Test
    void sinTextoSeListanTodos() {
        when(admin.esAdmin("admin")).thenReturn(true);
        when(admin.contar(null)).thenReturn(0L);

        assertThat(servicio.listar("", "admin", 0, 10).contenido()).isEmpty();
    }

    @Test
    void darElRolDeAdministrador() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA), Optional.of(LUCIA_ADMIN));

        assertThat(servicio.cambiarAdmin("lucia", true, "admin")).isEqualTo(LUCIA_ADMIN);
        verify(usuarios).asignarRol("lucia", "ADMIN");
    }

    @Test
    void elBloqueoSePideAntesQueNingunaOtraConsulta() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA), Optional.of(LUCIA_ADMIN));

        servicio.cambiarAdmin("lucia", true, "admin");

        InOrder orden = inOrder(admin, usuarios);
        orden.verify(admin).bloquearRolAdmin();
        orden.verify(admin).esAdmin("admin");
        orden.verify(admin).buscarPorId("lucia");
        orden.verify(usuarios).asignarRol("lucia", "ADMIN");
    }

    @Test
    void siYaTieneElRolNoCambiaNada() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA_ADMIN));

        assertThat(servicio.cambiarAdmin("lucia", true, "admin")).isEqualTo(LUCIA_ADMIN);
        verify(usuarios, never()).asignarRol(anyString(), anyString());
    }

    @Test
    void quitarElRolAOtroAdministrador() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA_ADMIN), Optional.of(LUCIA));
        when(admin.contarAdmins()).thenReturn(2L);

        assertThat(servicio.cambiarAdmin("lucia", false, "admin")).isEqualTo(LUCIA);
        verify(usuarios).quitarRol("lucia", "ADMIN");
    }

    @Test
    void unAdministradorNoPuedeQuitarseElRolASiMismo() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("ADMIN")).thenReturn(Optional.of(ADMIN));

        assertThatThrownBy(() -> servicio.cambiarAdmin("ADMIN", false, "admin"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCodigo()).isEqualTo("PROPIO_ROL");
                });
        verify(usuarios, never()).quitarRol(anyString(), anyString());
    }

    @Test
    void siempreDebeQuedarUnAdministrador() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA_ADMIN));
        when(admin.contarAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> servicio.cambiarAdmin("lucia", false, "admin"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("ULTIMO_ADMINISTRADOR");
                });
        verify(usuarios, never()).quitarRol(anyString(), anyString());
    }

    @Test
    void unSolicitanteQueYaNoEsAdministradorRecibe403() {
        when(admin.esAdmin("degradada")).thenReturn(false);

        assertThatThrownBy(() -> servicio.cambiarAdmin("admin", false, "degradada"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> servicio.listar("", "degradada", 0, 10)).isInstanceOf(ExcepcionApi.class);
        verify(admin, never()).buscarPorId(anyString());
    }

    @Test
    void unUsuarioInexistenteDa404() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarAdmin("nadie", true, "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void bloquearAUnUsuario() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA), Optional.of(LUCIA_BLOQUEADA));

        assertThat(servicio.cambiarBloqueo("lucia", true, "admin")).isEqualTo(LUCIA_BLOQUEADA);

        InOrder orden = inOrder(admin, usuarios);
        orden.verify(admin).bloquearRolAdmin();
        orden.verify(admin).esAdmin("admin");
        orden.verify(usuarios).actualizarBloqueado("lucia", true);
        // Al desbloquearla, sus tokens anteriores no vuelven a valer (D-089)
        orden.verify(usuarios).anularSesiones("lucia");
    }

    @Test
    void desbloquearYRepetirElMismoEstadoNoCambiaNada() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA_BLOQUEADA), Optional.of(LUCIA));

        assertThat(servicio.cambiarBloqueo("lucia", false, "admin")).isEqualTo(LUCIA);
        verify(usuarios).actualizarBloqueado("lucia", false);
        verify(usuarios, never()).anularSesiones(anyString());

        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA));
        assertThat(servicio.cambiarBloqueo("lucia", false, "admin")).isEqualTo(LUCIA);
        verify(usuarios).actualizarBloqueado(anyString(), anyBoolean());
    }

    @Test
    void niUnoMismoNiUnAdministradorSePuedenBloquear() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("admin")).thenReturn(Optional.of(ADMIN));
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA_ADMIN));

        assertThatThrownBy(() -> servicio.cambiarBloqueo("admin", true, "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("PROPIO_BLOQUEO"));
        assertThatThrownBy(() -> servicio.cambiarBloqueo("lucia", true, "admin"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("ADMIN_NO_BLOQUEABLE");
                });
        verify(usuarios, never()).actualizarBloqueado(anyString(), anyBoolean());
    }

    @Test
    void aUnUsuarioBloqueadoNoSeLeHaceAdministrador() {
        prepararSolicitanteAdmin();
        when(admin.buscarPorId("lucia")).thenReturn(Optional.of(LUCIA_BLOQUEADA));

        assertThatThrownBy(() -> servicio.cambiarAdmin("lucia", true, "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("USUARIO_BLOQUEADO"));
        verify(usuarios, never()).asignarRol(anyString(), anyString());
    }

    @Test
    void laModeracionDeRecetasExigeSerAdministradorEnLaBaseDeDatos() {
        when(admin.esAdmin("degradada")).thenReturn(false);

        assertThatThrownBy(() -> servicio.ocultarReceta(7, true, "Spam", "degradada"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> servicio.recetasOcultas("degradada", 0, 9)).isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.cambiarBloqueo("lucia", true, "degradada"))
                .isInstanceOf(ExcepcionApi.class);
        verifyNoInteractions(recetas);
    }

    @Test
    void laModeracionDeRecetasLaHaceElServicioDeRecetas() {
        prepararSolicitanteAdmin();

        servicio.ocultarReceta(7, true, "Spam", "admin");
        servicio.recetasOcultas("admin", 1, 9);

        verify(recetas).ocultar(7, true, "Spam", "admin");
        verify(recetas).ocultas("admin", 1, 9);
    }

    private void prepararSolicitanteAdmin() {
        when(admin.esAdmin("admin")).thenReturn(true);
    }
}
