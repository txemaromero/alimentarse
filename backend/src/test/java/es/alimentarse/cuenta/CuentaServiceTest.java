package es.alimentarse.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.alimentarse.admin.AdminMapper;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.cuenta.CuentaMapper.FilaActividad;
import es.alimentarse.cuenta.CuentaMapper.FilaIngrediente;
import es.alimentarse.cuenta.CuentaMapper.FilaReceta;
import es.alimentarse.imagen.ImagenService;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-07T18:00:00Z");
    private static final Instant AYER = AHORA.minusSeconds(86_400);

    @Mock
    CuentaMapper cuenta;

    @Mock
    UsuarioMapper usuarios;

    @Mock
    AdminMapper admin;

    @Mock
    ImagenService imagenes;

    @Mock
    ApplicationEventPublisher eventos;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private CuentaService servicio;
    private Usuario lucia;

    @BeforeEach
    void crearServicio() {
        servicio = new CuentaService(
                cuenta, usuarios, admin, imagenes, passwordEncoder, eventos, Clock.fixed(AHORA, ZoneOffset.UTC));
        lucia = new Usuario(
                "lucia",
                "Lucía",
                "Fernández",
                null,
                "lucia@toca.local",
                passwordEncoder.encode("demo1234"),
                null,
                null,
                false,
                null,
                true,
                null);
    }

    @Test
    void laExportacionReuneElPerfilLasRecetasConSusIngredientesLosConsumosYLosMeGusta() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));
        when(usuarios.rolesDe("lucia")).thenReturn(List.of("USER"));
        when(cuenta.recetasDe("lucia"))
                .thenReturn(List.of(
                        fila(1, "Tortilla", "foto.jpg"), fila(2, "Ensalada", null), fila(3, "Sin ingredientes", null)));
        when(cuenta.ingredientesDe("lucia"))
                .thenReturn(List.of(
                        new FilaIngrediente(1, "Huevos", "4", "Huevos"),
                        new FilaIngrediente(1, "Patata", "500 gramos", "Hortalizas"),
                        new FilaIngrediente(2, "Lechuga", "1", "Verduras")));
        when(cuenta.consumosDe("lucia")).thenReturn(List.of(new FilaActividad(AYER, 9, "Lentejas")));
        when(cuenta.planDe("lucia")).thenReturn(List.of(new FilaActividad(AYER, 5, "Pollo")));
        when(cuenta.meGustaDe("lucia")).thenReturn(List.of(new FilaActividad(AYER, 8, "Batido")));
        when(cuenta.valoracionesDe("lucia"))
                .thenReturn(List.of(new CuentaMapper.FilaValoracion(AYER, 8, "Batido", 4, "Muy rico", false)));
        when(cuenta.suscripcionesDe("lucia"))
                .thenReturn(List.of(new CuentaMapper.FilaSuscripcion(AYER, "https://fcm.googleapis.com/fcm/send/x")));

        DatosPersonalesDto datos = servicio.exportar("lucia");

        assertThat(datos.exportado()).isEqualTo(AHORA);
        assertThat(datos.perfil().email()).isEqualTo("lucia@toca.local");
        assertThat(datos.recetas())
                .extracting(DatosPersonalesDto.Receta::titulo)
                .containsExactly("Tortilla", "Ensalada", "Sin ingredientes");
        assertThat(datos.recetas().get(0).imagen()).isEqualTo("/api/imagenes/foto.jpg");
        assertThat(datos.recetas().get(0).ingredientes())
                .containsExactly(
                        new DatosPersonalesDto.Ingrediente("Huevos", "4", "Huevos"),
                        new DatosPersonalesDto.Ingrediente("Patata", "500 gramos", "Hortalizas"));
        assertThat(datos.recetas().get(2).ingredientes()).isEmpty();
        assertThat(datos.consumos()).containsExactly(new DatosPersonalesDto.Actividad(AYER, 9, "Lentejas"));
        assertThat(datos.meGusta()).containsExactly(new DatosPersonalesDto.Actividad(AYER, 8, "Batido"));
        assertThat(datos.comidasPlanificadas()).containsExactly(new DatosPersonalesDto.Actividad(AYER, 5, "Pollo"));
        assertThat(datos.valoraciones())
                .containsExactly(new DatosPersonalesDto.Valoracion(AYER, 8, "Batido", 4, "Muy rico", false));
        // Solo el servicio: la dirección completa da acceso al dispositivo
        assertThat(datos.avisos()).containsExactly(new DatosPersonalesDto.Aviso(AYER, "fcm.googleapis.com"));
    }

    @Test
    void laBajaBloqueaPrimeroYBorraTodoEnOrden() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));
        when(cuenta.imagenesDe("lucia")).thenReturn(List.of("a.jpg", "b.png"));

        servicio.darDeBaja("lucia", "demo1234");

        InOrder orden = inOrder(admin, cuenta, usuarios);
        orden.verify(admin).bloquearRolAdmin();
        orden.verify(cuenta).bloquearRecetasDe("lucia");
        orden.verify(usuarios).buscarPorId("lucia");
        orden.verify(cuenta).borrarConsumosDe("lucia");
        orden.verify(cuenta).borrarPlanDe("lucia");
        orden.verify(cuenta).borrarLikesDe("lucia");
        orden.verify(cuenta).borrarLikesDeSusRecetas("lucia");
        orden.verify(cuenta).borrarValoracionesDe("lucia");
        orden.verify(cuenta).borrarSuscripcionesDe("lucia");
        // Las consumidas por otros se quedan, pero sin fotos (D-083)
        orden.verify(cuenta).borrarFotosDe("lucia");
        orden.verify(cuenta).anonimizarRecetasConsumidas("lucia");
        orden.verify(cuenta).borrarRecetasDe("lucia");
        orden.verify(cuenta).borrarRolesDe("lucia");
        orden.verify(cuenta).borrarUsuario("lucia");
        verify(imagenes).borrarTrasCommit("a.jpg");
        verify(imagenes).borrarTrasCommit("b.png");
        verify(eventos).publishEvent(new CuentaEliminada("Lucía", "lucia@toca.local"));
    }

    @Test
    void conUnaContrasenaIncorrectaNoBorraNada() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));

        assertThatThrownBy(() -> servicio.darDeBaja("lucia", "otra")).isInstanceOfSatisfying(ExcepcionApi.class, e -> {
            assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getErrores()).containsOnlyKeys("password");
        });
        verify(cuenta, never()).borrarUsuario(anyString());
        verifyNoInteractions(eventos, imagenes);
    }

    @Test
    void elUltimoAdministradorNoPuedeDarseDeBaja() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));
        when(admin.esAdmin("lucia")).thenReturn(true);
        when(admin.contarAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> servicio.darDeBaja("lucia", "demo1234"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("ULTIMO_ADMINISTRADOR");
                });
        verify(cuenta, never()).borrarUsuario(anyString());
    }

    @Test
    void unAdministradorPuedeDarseDeBajaSiQuedanOtros() {
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(lucia));
        when(admin.esAdmin("lucia")).thenReturn(true);
        when(admin.contarAdmins()).thenReturn(2L);

        servicio.darDeBaja("lucia", "demo1234");

        verify(cuenta).borrarUsuario("lucia");
        verify(eventos).publishEvent(any(CuentaEliminada.class));
    }

    private static FilaReceta fila(int id, String titulo, String imagen) {
        return new FilaReceta(
                id, titulo, "Plato", "<p>Paso</p>", 10, 2, "Fácil", true, false, null, AYER, imagen, null);
    }
}
