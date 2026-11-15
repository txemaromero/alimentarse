package es.alimentarse.receta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.alimentarse.catalogo.TipoRecetaDto;
import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.error.ExcepcionApi;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Valoraciones de las recetas (D-077). */
@ExtendWith(MockitoExtension.class)
class ValoracionServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-13T18:30:00.750Z");
    private static final Instant AHORA_SIN_FRACCION = Instant.parse("2026-11-13T18:30:00Z");

    @Mock
    RecetaService recetas;

    @Mock
    ValoracionMapper valoraciones;

    private ValoracionService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ValoracionService(recetas, valoraciones, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void guardaLaValoracionYDevuelveComoQuedaLaReceta() {
        when(recetas.visible(1, "ana", false)).thenReturn(receta(1, "autora"));
        ValoracionDto mia = new ValoracionDto(7, "ana", 4, "Muy rica", AHORA_SIN_FRACCION, false);
        when(valoraciones.notaMedia(1)).thenReturn(new NotaMedia(4.5, 2));
        when(valoraciones.deUsuario(1, "ana")).thenReturn(Optional.of(mia));

        ResumenValoracionesDto resumen = servicio.valorar(1, new ValoracionRequest(4, "  Muy rica  "), "ana");

        verify(valoraciones).guardar("ana", 1, 4, "Muy rica", AHORA_SIN_FRACCION);
        assertThat(resumen).isEqualTo(new ResumenValoracionesDto(4.5, 2, mia));
    }

    @Test
    void nadieValoraSuPropiaReceta() {
        when(recetas.visible(1, "autora", false)).thenReturn(receta(1, "autora"));

        assertThatThrownBy(() -> servicio.valorar(1, new ValoracionRequest(5, null), "autora"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getCodigo()).isEqualTo("PROPIA_RECETA");
                });
        verify(valoraciones, never()).guardar(anyString(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void unaRecetaQueNoSeVeNoSeValoraNiSeListaNiSeBorra() {
        when(recetas.visible(eq(9), anyString(), eq(false)))
                .thenThrow(ExcepcionApi.noEncontrado("La receta no existe."));

        assertThatThrownBy(() -> servicio.valorar(9, new ValoracionRequest(5, null), "ana"))
                .isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.borrar(9, "ana")).isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.listar(9, "ana", false, 0, 5)).isInstanceOf(ExcepcionApi.class);
        verify(valoraciones, never()).borrar(anyInt(), anyString());
    }

    @Test
    void borrarLaPropiaEsIdempotente() {
        when(recetas.visible(1, "ana", false)).thenReturn(receta(1, "autora"));
        when(valoraciones.notaMedia(1)).thenReturn(new NotaMedia(null, 0));

        assertThat(servicio.borrar(1, "ana")).isEqualTo(new ResumenValoracionesDto(null, 0, null));
        verify(valoraciones).borrar(1, "ana");
    }

    @Test
    void listaPorPaginasYSoloLosAdministradoresVenLasOcultas() {
        ValoracionDto una = new ValoracionDto(3, "benito", 5, null, AHORA, true);
        when(valoraciones.contar(1, true)).thenReturn(6L);
        when(valoraciones.deReceta(1, true, 5, 5)).thenReturn(List.of(una));

        Pagina<ValoracionDto> pagina = servicio.listar(1, "moderadora", true, 1, 5);

        verify(recetas).visible(1, "moderadora", true);
        assertThat(pagina.contenido()).containsExactly(una);
        assertThat(pagina.totalElementos()).isEqualTo(6);
    }

    @Test
    void unaPaginaFueraDeRangoNoConsultaLasFilas() {
        when(valoraciones.contar(1, false)).thenReturn(3L);

        assertThat(servicio.listar(1, "ana", false, 2, 5).contenido()).isEmpty();
        verify(valoraciones, never()).deReceta(anyInt(), eq(false), anyLong(), anyInt());
    }

    @Test
    void ocultaUnaValoracionQueExiste() {
        ValoracionDto oculta = new ValoracionDto(3, "benito", 1, "Spam", AHORA, true);
        when(valoraciones.actualizarOculta(3, true)).thenReturn(1);
        when(valoraciones.buscar(3)).thenReturn(Optional.of(oculta));

        assertThat(servicio.ocultar(3, true)).isEqualTo(oculta);
    }

    @Test
    void ocultarUnaQueNoExisteEsUn404() {
        when(valoraciones.actualizarOculta(99, true)).thenReturn(0);

        assertThatThrownBy(() -> servicio.ocultar(99, true))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void elComentarioQuedaEnTextoPlanoSinCaracteresDeControl() {
        assertThat(ValoracionService.comentario(null)).isNull();
        assertThat(ValoracionService.comentario("   ")).isNull();
        assertThat(ValoracionService.comentario(" Rica\r\nY fácil" + (char) 7 + " "))
                .isEqualTo("Rica\nY fácil");
        assertThat(ValoracionService.comentario("<b>sin</b> HTML")).isEqualTo("<b>sin</b> HTML");
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static RecetaBase receta(int id, String autor) {
        return new RecetaBase(
                id,
                "Tortilla",
                "<p>Paso</p>",
                autor,
                new TipoRecetaDto(1, "Plato"),
                true,
                false,
                false,
                null,
                null,
                30,
                4,
                "Fácil",
                0,
                false,
                null,
                0,
                Instant.parse("2026-11-01T10:00:00Z"));
    }
}
