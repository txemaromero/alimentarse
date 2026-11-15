package es.alimentarse.ingrediente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.alimentarse.catalogo.CatalogoMapper;
import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.seguridad.LimiteIntentosService;
import es.alimentarse.seguridad.UsuarioAutenticado;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class IngredienteServiceTest {

    private static final GrupoDto LEGUMBRES = new GrupoDto(6, "Legumbres", 4);
    private static final IngredienteDto QUINOA = new IngredienteDto(100, "Quinoa", LEGUMBRES, null);

    @Mock
    IngredienteMapper ingredientes;

    @Mock
    CatalogoMapper catalogo;

    @Mock
    UsuarioMapper usuarios;

    @Mock
    LimiteIntentosService limite;

    private static final UsuarioAutenticado ANA = new UsuarioAutenticado("ana", "Ana", List.of("USER"));
    private static final UsuarioAutenticado GESTORA =
            new UsuarioAutenticado("gestora", "Gestora", List.of("USER", "ADMIN"));

    private IngredienteService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new IngredienteService(ingredientes, catalogo, usuarios, limite);
    }

    /** Ana, con el email confirmado o sin confirmar (D-088). */
    private void emailDeAna(boolean confirmado) {
        when(usuarios.buscarPorId("ana"))
                .thenReturn(Optional.of(new Usuario(
                        "ana", "Ana", "Ruiz", null, "ana@toca.local", "x", null, null, false, null, confirmado, null)));
    }

    @Test
    void listarCalculaElDesplazamientoYElTotalDePaginas() {
        when(ingredientes.contar("%ac%", null, false)).thenReturn(77L);
        when(ingredientes.buscar("%ac%", null, false, 32L, 16)).thenReturn(List.of(QUINOA));

        Pagina<IngredienteDto> pagina = servicio.listar(" ac ", null, false, 2, 16);

        assertThat(pagina).isEqualTo(new Pagina<>(List.of(QUINOA), 2, 16, 77, 5));
    }

    @Test
    void unaPaginaFueraDeRangoVieneVaciaSinConsultarLasFilas() {
        when(ingredientes.contar(null, 6, true)).thenReturn(4L);

        Pagina<IngredienteDto> pagina = servicio.listar(null, 6, true, 1, 16);

        assertThat(pagina.contenido()).isEmpty();
        assertThat(pagina.totalElementos()).isEqualTo(4);
        verify(ingredientes, never()).buscar(null, 6, true, 16L, 16);
    }

    @Test
    void lasSugerenciasPidenHastaDiezConPrefijoYContiene() {
        when(ingredientes.sugerencias("to%", "%to%", 10)).thenReturn(List.of(QUINOA));

        assertThat(servicio.sugerencias("to")).containsExactly(QUINOA);
    }

    @Test
    void sinTextoNoHaySugerenciasNiConsulta() {
        assertThat(servicio.sugerencias("  ")).isEmpty();
        verifyNoInteractions(ingredientes);
    }

    @Test
    void crearRecortaElNombreYDevuelveElIngredienteCreado() {
        emailDeAna(true);
        when(catalogo.existeGrupo(6)).thenReturn(true);
        when(ingredientes.ultimoIdInsertado()).thenReturn(100);
        when(ingredientes.buscarPorId(100)).thenReturn(Optional.of(QUINOA));

        assertThat(servicio.crear(new IngredienteRequest("  Quinoa ", 6), ANA)).isEqualTo(QUINOA);
        verify(ingredientes).insertar("Quinoa", 6);
        // Cuenta en su tope diario (D-089)
        verify(limite).registrarIntento("ingrediente:ana", IngredienteService.LIMITE_NUEVOS);
    }

    @Test
    void sinElEmailConfirmadoNoSeCreaNada() {
        emailDeAna(false);

        assertThatThrownBy(() -> servicio.crear(new IngredienteRequest("Quinoa", 6), ANA))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getCodigo()).isEqualTo("EMAIL_SIN_CONFIRMAR");
                });
        verify(ingredientes, never()).insertar(anyString(), anyInt());
        verifyNoInteractions(limite);
    }

    @Test
    void conElTopeDiarioAlcanzadoDevuelve429ConSuMensaje() {
        emailDeAna(true);
        when(catalogo.existeGrupo(6)).thenReturn(true);
        doThrow(ExcepcionApi.demasiadosIntentos(Duration.ofHours(3)))
                .when(limite)
                .registrarIntento("ingrediente:ana", IngredienteService.LIMITE_NUEVOS);

        assertThatThrownBy(() -> servicio.crear(new IngredienteRequest("Quinoa", 6), ANA))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getCodigo()).isEqualTo("LIMITE_INGREDIENTES");
                    assertThat(e.getMessage()).isEqualTo(IngredienteService.DEMASIADOS_NUEVOS);
                    assertThat(e.segundosParaReintentar()).isEqualTo("10800");
                });
        verify(ingredientes, never()).insertar(anyString(), anyInt());
    }

    @Test
    void unAdministradorNoNecesitaElEmailConfirmadoNiTieneTope() {
        when(catalogo.existeGrupo(6)).thenReturn(true);
        when(ingredientes.ultimoIdInsertado()).thenReturn(100);
        when(ingredientes.buscarPorId(100)).thenReturn(Optional.of(QUINOA));

        assertThat(servicio.crear(new IngredienteRequest("Quinoa", 6), GESTORA)).isEqualTo(QUINOA);
        verifyNoInteractions(usuarios, limite);
    }

    @Test
    void crearConUnNombreQueYaExisteDevuelve409() {
        emailDeAna(true);
        when(catalogo.existeGrupo(6)).thenReturn(true);
        when(ingredientes.existeNombre("lentejas", null)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new IngredienteRequest("lentejas", 6), ANA))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getErrores()).containsEntry("nombre", IngredienteService.NOMBRE_EN_USO);
                });
        verify(ingredientes, never()).insertar(anyString(), anyInt());
    }

    @Test
    void crearConUnGrupoInexistenteDevuelve400() {
        emailDeAna(true);
        when(catalogo.existeGrupo(999)).thenReturn(false);

        assertThatThrownBy(() -> servicio.crear(new IngredienteRequest("Quinoa", 999), ANA))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores()).containsEntry("grupo", IngredienteService.GRUPO_INEXISTENTE);
                });
    }

    @Test
    void editarUnIngredienteQueNoExisteDevuelve404() {
        when(ingredientes.buscarPorId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.editar(999, new IngredienteRequest("Quinoa", 6)))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void editarComprobandoElNombreSinContarseASiMismo() {
        when(ingredientes.buscarPorId(100)).thenReturn(Optional.of(QUINOA));
        when(catalogo.existeGrupo(6)).thenReturn(true);
        when(ingredientes.existeNombre("Quinoa real", 100)).thenReturn(false);

        servicio.editar(100, new IngredienteRequest(" Quinoa real ", 6));

        verify(ingredientes).actualizar(100, "Quinoa real", 6);
    }
}
