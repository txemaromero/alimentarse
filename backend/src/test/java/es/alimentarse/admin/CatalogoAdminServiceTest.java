package es.alimentarse.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.ingrediente.IngredienteDto;
import es.alimentarse.ingrediente.IngredienteMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CatalogoAdminServiceTest {

    private static final GrupoAdminDto CARNE = new GrupoAdminDto(11, "Carne", 3, false, 12);
    private static final IngredienteDto TOMATE =
            new IngredienteDto(5, "Tomate", new GrupoDto(4, "Hortalizas", null), "Tomate");
    private static final IngredienteDto TOMATE_PERA =
            new IngredienteDto(90, "Tomate pera", new GrupoDto(4, "Hortalizas", null), null);

    @Mock
    CatalogoAdminMapper catalogo;

    @Mock
    IngredienteMapper ingredientes;

    @Mock
    AdminService admin;

    @InjectMocks
    CatalogoAdminService servicio;

    @Test
    void todoExigeSerAdministradorEnLaBaseDeDatos() {
        doThrow(ExcepcionApi.prohibido("No")).when(admin).exigirAdministrador("degradada");

        assertThatThrownBy(() -> servicio.grupos("degradada")).isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.crearGrupo(new GrupoRequest("X", 1, false), "degradada"))
                .isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.borrarGrupo(11, "degradada")).isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.fusionarIngredientes(90, 5, "degradada"))
                .isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.borrarIngrediente(90, "degradada")).isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.buscarAlimentos("harina", "degradada"))
                .isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() ->
                        servicio.guardarNutricion(90, new NutricionIngredienteRequest(2677, List.of()), "degradada"))
                .isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.nutricion(90, "degradada")).isInstanceOf(ExcepcionApi.class);
        verifyNoInteractions(catalogo, ingredientes);
    }

    @Test
    void crearUnGrupoSinEspaciosYConElNombreLibre() {
        when(catalogo.existeNombreGrupo("Marisco", null)).thenReturn(false);
        when(catalogo.ultimoIdInsertado()).thenReturn(20);
        GrupoAdminDto marisco = new GrupoAdminDto(20, "Marisco", 2, false, 0);
        when(catalogo.grupoPorId(20)).thenReturn(Optional.of(marisco));

        assertThat(servicio.crearGrupo(new GrupoRequest(" Marisco ", 2, false), "admin"))
                .isEqualTo(marisco);
        verify(catalogo).insertarGrupo("Marisco", 2, false);
    }

    @Test
    void unNombreDeGrupoRepetidoDa409EnElCampo() {
        when(catalogo.grupoPorId(11)).thenReturn(Optional.of(CARNE));
        when(catalogo.existeNombreGrupo("Pescado", 11)).thenReturn(true);

        assertThatThrownBy(() -> servicio.editarGrupo(11, new GrupoRequest("Pescado", 3, false), "admin"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getErrores()).containsOnlyKeys("nombre");
                });
        verify(catalogo, never())
                .actualizarGrupo(anyInt(), anyString(), org.mockito.ArgumentMatchers.any(), anyBoolean());
    }

    @Test
    void editarLasRacionesDeUnGrupo() {
        when(catalogo.grupoPorId(11))
                .thenReturn(Optional.of(CARNE), Optional.of(new GrupoAdminDto(11, "Carne", null, false, 12)));

        assertThat(servicio.editarGrupo(11, new GrupoRequest("Carne", null, false), "admin")
                        .racionesSemana())
                .isNull();
        verify(catalogo).actualizarGrupo(11, "Carne", null, false);
    }

    @Test
    void unGrupoInexistenteDa404() {
        when(catalogo.grupoPorId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.editarGrupo(99, new GrupoRequest("X", null, false), "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> servicio.borrarGrupo(99, "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void soloSeBorraUnGrupoSinIngredientes() {
        when(catalogo.grupoPorId(11)).thenReturn(Optional.of(CARNE));
        when(catalogo.contarIngredientesDeGrupo(11)).thenReturn(12L);
        when(catalogo.grupoPorId(20)).thenReturn(Optional.of(new GrupoAdminDto(20, "Marisco", 2, false, 0)));
        when(catalogo.contarIngredientesDeGrupo(20)).thenReturn(0L);

        assertThatThrownBy(() -> servicio.borrarGrupo(11, "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("GRUPO_EN_USO"));
        servicio.borrarGrupo(20, "admin");

        verify(catalogo, never()).borrarGrupo(11);
        verify(catalogo).borrarGrupo(20);
    }

    @Test
    void fusionarBloqueaYMueveLasRecetasAlDestino() {
        when(ingredientes.buscarPorId(90)).thenReturn(Optional.of(TOMATE_PERA));
        when(ingredientes.buscarPorId(5)).thenReturn(Optional.of(TOMATE));
        when(catalogo.contarRecetasConIngrediente(90)).thenReturn(3);

        assertThat(servicio.fusionarIngredientes(90, 5, "admin")).isEqualTo(new FusionDto(TOMATE, 3));

        InOrder orden = inOrder(catalogo);
        orden.verify(catalogo).bloquearIngredientes(90, 5);
        orden.verify(catalogo).contarRecetasConIngrediente(90);
        orden.verify(catalogo).sumarCantidades(90, 5);
        orden.verify(catalogo).borrarLineasRepetidas(90, 5);
        orden.verify(catalogo).moverLineas(90, 5);
        // El destino hereda el alimento de BEDCA y los pesos de las piezas que no tenga (D-080)
        orden.verify(catalogo).heredarAlimento(90, 5);
        orden.verify(catalogo).heredarPiezas(90, 5);
        orden.verify(catalogo).borrarIngrediente(90);
    }

    @Test
    void noSeFusionaUnIngredienteConsigoMismoNiConUnoQueNoExiste() {
        assertThatThrownBy(() -> servicio.fusionarIngredientes(5, 5, "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getErrores()).containsOnlyKeys("destino"));
        when(ingredientes.buscarPorId(90)).thenReturn(Optional.of(TOMATE_PERA));
        when(ingredientes.buscarPorId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.fusionarIngredientes(90, 999, "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(catalogo, never()).moverLineas(anyInt(), anyInt());
    }

    @Test
    void soloSeBorraUnIngredienteQueNoUsaNingunaReceta() {
        when(ingredientes.buscarPorId(90)).thenReturn(Optional.of(TOMATE_PERA));
        when(catalogo.contarRecetasConIngrediente(90)).thenReturn(2, 0);

        assertThatThrownBy(() -> servicio.borrarIngrediente(90, "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("INGREDIENTE_EN_USO"));
        verify(catalogo, never()).borrarIngrediente(90);

        servicio.borrarIngrediente(90, "admin");
        verify(catalogo, org.mockito.Mockito.times(2)).bloquearIngredientes(90, 90);
        verify(catalogo).borrarIngrediente(90);
    }

    @Test
    void laBusquedaDeAlimentosExigeTodasLasPalabrasYQuitaLosCerosDeRelleno() {
        AlimentoBedca pollo = new AlimentoBedca(
                2292,
                "Pollo, entero, con piel, crudo",
                new BigDecimal("696.700000"),
                new BigDecimal("9.250000"),
                null,
                new BigDecimal("0.000000"),
                new BigDecimal("0.000000"),
                new BigDecimal("20.850000"),
                new BigDecimal("63.000000"));
        when(catalogo.buscarAlimentos(List.of("%pollo%", "%crudo%"), "pollo, crudo%", 10))
                .thenReturn(List.of(pollo));

        List<AlimentoBedcaDto> alimentos = servicio.buscarAlimentos("  pollo, crudo ", "admin");

        assertThat(alimentos).hasSize(1);
        AlimentoBedcaDto dto = alimentos.getFirst();
        assertThat(dto.id()).isEqualTo(2292);
        assertThat(dto.energiaKcal()).isEqualByComparingTo("167"); // 696,7 / 4,184
        assertThat(dto.energiaKj()).hasToString("696.7");
        assertThat(dto.grasasSaturadas()).isNull();
        assertThat(dto.hidratos()).hasToString("0");
        assertThat(dto.sodioMg()).hasToString("63");
    }

    @Test
    void sinTextoNoSeBuscanAlimentos() {
        assertThat(servicio.buscarAlimentos("  , ", "admin")).isEmpty();
        verifyNoInteractions(catalogo);
    }

    @Test
    void sinCerosNoUsaNotacionCientifica() {
        assertThat(CatalogoAdminService.sinCeros(new BigDecimal("35000.000000")))
                .hasToString("35000");
        assertThat(CatalogoAdminService.sinCeros(new BigDecimal("0.172121"))).hasToString("0.172121");
        assertThat(CatalogoAdminService.sinCeros(null)).isNull();
    }

    @Test
    void guardarBloqueaElIngredienteYSustituyeAlimentoYPiezas() {
        when(ingredientes.buscarPorId(90)).thenReturn(Optional.of(TOMATE_PERA));
        when(catalogo.existeAlimento(2421)).thenReturn(true);

        assertThat(servicio.guardarNutricion(90, datos(2421, pieza(" Unidad ", "120")), "admin"))
                .isEqualTo(TOMATE_PERA);

        InOrder orden = inOrder(catalogo);
        orden.verify(catalogo).bloquearIngredientes(90, 90);
        orden.verify(catalogo).existeAlimento(2421);
        orden.verify(catalogo).asignarAlimento(90, 2421);
        orden.verify(catalogo).borrarPiezas(90);
        // La pieza se guarda sin espacios y en minúsculas
        orden.verify(catalogo).insertarPiezas(90, List.of(pieza("unidad", "120")));
        verify(catalogo, never()).quitarAlimento(anyInt());
    }

    @Test
    void sinAlimentoNiPiezasSeLeQuitanLosDatos() {
        when(ingredientes.buscarPorId(5)).thenReturn(Optional.of(TOMATE));

        servicio.guardarNutricion(5, datos(null), "admin");

        verify(catalogo).quitarAlimento(5);
        verify(catalogo).borrarPiezas(5);
        verify(catalogo, never()).asignarAlimento(anyInt(), anyInt());
        verify(catalogo, never()).insertarPiezas(anyInt(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void unAlimentoQueNoEstaEnBedcaYPiezasRepetidasOGeneralesDan400EnSusCampos() {
        when(ingredientes.buscarPorId(90)).thenReturn(Optional.of(TOMATE_PERA));

        assertThatThrownBy(() -> servicio.guardarNutricion(
                        90, datos(1, pieza("lata", "60"), pieza("Latas", "80"), pieza("gramos", "1")), "admin"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores())
                            .containsOnly(
                                    entry("alimento", CatalogoAdminService.ALIMENTO_INEXISTENTE),
                                    entry("piezas[1].pieza", CatalogoAdminService.PIEZA_REPETIDA),
                                    entry("piezas[2].pieza", CatalogoAdminService.PIEZA_GENERAL));
                });
        verify(catalogo, never()).asignarAlimento(anyInt(), anyInt());
        verify(catalogo, never()).borrarPiezas(anyInt());
    }

    @Test
    void guardarEnUnIngredienteQueNoExisteDa404() {
        assertThatThrownBy(() -> servicio.guardarNutricion(404, datos(2421), "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(catalogo, never()).asignarAlimento(anyInt(), anyInt());
    }

    @Test
    void losDatosNutricionalesTraenElAlimentoConSusValoresYLasPiezas() {
        when(ingredientes.buscarPorId(5)).thenReturn(Optional.of(TOMATE));
        when(catalogo.alimentoDe(5))
                .thenReturn(Optional.of(new AlimentoBedca(
                        2421, "Tomate", new BigDecimal("78.500000"), null, null, null, null, null, null)));
        when(catalogo.piezasDe(5)).thenReturn(List.of(pieza("unidad", "120")));

        NutricionIngredienteDto nutricion = servicio.nutricion(5, "admin");

        assertThat(nutricion.alimento()).isNotNull();
        assertThat(nutricion.alimento().nombre()).isEqualTo("Tomate");
        assertThat(nutricion.alimento().energiaKj()).hasToString("78.5");
        assertThat(nutricion.piezas()).containsExactly(pieza("unidad", "120"));

        when(catalogo.alimentoDe(5)).thenReturn(Optional.empty());
        assertThat(servicio.nutricion(5, "admin").alimento()).isNull();
        assertThatThrownBy(() -> servicio.nutricion(404, "admin")).isInstanceOf(ExcepcionApi.class);
    }

    private static NutricionIngredienteRequest datos(Integer alimento, PiezaDto... piezas) {
        return new NutricionIngredienteRequest(alimento, List.of(piezas));
    }

    private static PiezaDto pieza(String nombre, String gramos) {
        return new PiezaDto(nombre, new BigDecimal(gramos));
    }

    @Test
    void laListaDeGruposEsLaDelMapper() {
        when(catalogo.grupos()).thenReturn(List.of(CARNE));

        assertThat(servicio.grupos("admin")).containsExactly(CARNE);
    }
}
