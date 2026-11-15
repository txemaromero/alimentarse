package es.alimentarse.receta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.alimentarse.catalogo.CatalogoMapper;
import es.alimentarse.catalogo.GrupoDto;
import es.alimentarse.catalogo.TipoRecetaDto;
import es.alimentarse.comun.Pagina;
import es.alimentarse.comun.SaneadorHtml;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.imagen.ImagenService;
import es.alimentarse.support.ImagenesDePrueba;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class RecetaServiceTest {

    private static final GrupoDto CARNE = new GrupoDto(11, "Carne", 3);
    private static final GrupoDto PASTA = new GrupoDto(7, "Pasta", 4);
    private static final GrupoDto HORTALIZAS = new GrupoDto(4, "Hortalizas", null);
    private static final GrupoDto ESENCIALES = new GrupoDto(1, "Esenciales", null);
    private static final Instant AHORA = Instant.parse("2026-11-07T18:00:00Z");
    private static final java.time.Clock RELOJ = java.time.Clock.fixed(AHORA, java.time.ZoneOffset.UTC);

    @Mock
    RecetaMapper recetas;

    @Mock
    ValoracionMapper valoraciones;

    @Mock
    CatalogoMapper catalogo;

    @Mock
    ImagenService imagenes;

    private RecetaService servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new RecetaService(recetas, valoraciones, catalogo, imagenes, new SaneadorHtml(), RELOJ);
    }

    @Test
    void elMaximoSemanalEsElMinimoDeLosGruposConLimite() {
        assertThat(RecetaService.maxSemanal(List.of(HORTALIZAS, CARNE, PASTA, ESENCIALES)))
                .isEqualTo(3);
        assertThat(RecetaService.maxSemanal(List.of(PASTA, HORTALIZAS))).isEqualTo(4);
    }

    @Test
    void sinGruposConLimiteElMaximoSemanalEsIlimitado() {
        assertThat(RecetaService.maxSemanal(List.of(HORTALIZAS, ESENCIALES))).isNull();
        assertThat(RecetaService.maxSemanal(List.of())).isNull();
    }

    @Test
    void crearGuardaLaRecetaSinPublicarConElHtmlSaneadoYSusIngredientes() {
        prepararValidacionCorrecta(Set.of(3, 5));
        when(recetas.ultimoIdInsertado()).thenReturn(42);
        when(recetas.buscarBase(42, "lucia")).thenReturn(Optional.of(base(42, "lucia", false, null)));

        RecetaDetalleDto creada = servicio.crear(
                peticion("<p>Bate</p><script>alert(1)</script>", ingrediente(3, " 2 unidades "), ingrediente(5, "sal")),
                null,
                "lucia");

        ArgumentCaptor<String> instrucciones = ArgumentCaptor.forClass(String.class);
        verify(recetas)
                .insertar(
                        eq("Tortilla"),
                        instrucciones.capture(),
                        eq("lucia"),
                        eq(1),
                        isNull(),
                        eq(30),
                        eq(4),
                        eq("Fácil"));
        assertThat(instrucciones.getValue()).isEqualTo("<p>Bate</p>");
        verify(recetas).insertarIngredientes(42, List.of(ingrediente(3, "2 unidades"), ingrediente(5, "sal")));
        assertThat(creada.publicada()).isFalse();
        assertThat(creada.esAutor()).isTrue();
    }

    @Test
    void crearConFotosLasGuardaEnElOrdenPedidoYLaPrimeraEsLaPrincipal() {
        prepararValidacionCorrecta(Set.of(3));
        MockMultipartFile a = ImagenesDePrueba.parteFoto("a.png", ImagenesDePrueba.png());
        MockMultipartFile b = ImagenesDePrueba.parteFoto("b.png", ImagenesDePrueba.png());
        when(imagenes.guardar(b, "fotos[0]")).thenReturn("b.jpg");
        when(imagenes.guardar(a, "fotos[1]")).thenReturn("a.jpg");
        when(recetas.ultimoIdInsertado()).thenReturn(42);
        when(recetas.buscarBase(42, "lucia")).thenReturn(Optional.of(base(42, "lucia", false, "b.jpg")));
        when(recetas.fotosDe(42))
                .thenReturn(List.of(new FotoReceta("b.jpg", 0, "Cortada"), new FotoReceta("a.jpg", 1, null)));

        RecetaDetalleDto creada = servicio.crear(
                peticion(List.of(nueva(1, " Cortada "), nueva(0, "  ")), "<p>Paso</p>", ingrediente(3, "1")),
                List.of(a, b),
                "lucia");

        verify(recetas)
                .insertar(
                        anyString(), anyString(), eq("lucia"), anyInt(), eq("b.jpg"), anyInt(), anyInt(), anyString());
        verify(recetas)
                .insertarFotos(42, List.of(new FotoReceta("b.jpg", 0, "Cortada"), new FotoReceta("a.jpg", 1, null)));
        assertThat(creada.imagen()).isEqualTo("/api/imagenes/b.jpg");
        assertThat(creada.fotos())
                .containsExactly(
                        new FotoDto("b.jpg", "/api/imagenes/b.jpg", "Cortada"),
                        new FotoDto("a.jpg", "/api/imagenes/a.jpg", null));
    }

    @Test
    void laGaleriaPedidaSeValidaFotoAFotoAntesDeGuardarNingunFichero() {
        List<FotoReceta> actuales = List.of(new FotoReceta("mia.jpg", 0, null));
        MockMultipartFile fichero = ImagenesDePrueba.parteFoto("a.png", ImagenesDePrueba.png());

        assertThatThrownBy(() -> servicio.galeria(
                        List.of(
                                new FotoRequest(null, null, null),
                                new FotoRequest("mia.jpg", 0, null),
                                existente("ajena.jpg"),
                                existente("mia.jpg"),
                                existente("mia.jpg"),
                                nueva(3, null),
                                nueva(0, null),
                                nueva(0, null)),
                        List.of(fichero),
                        actuales))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores())
                                .containsOnly(
                                        entry("fotos[0]", RecetaService.FOTO_SIN_ORIGEN),
                                        entry("fotos[1]", RecetaService.FOTO_SIN_ORIGEN),
                                        entry("fotos[2].existente", RecetaService.FOTO_AJENA),
                                        entry("fotos[4].existente", RecetaService.FOTO_REPETIDA),
                                        entry("fotos[5].nueva", RecetaService.FICHERO_QUE_FALTA),
                                        entry("fotos[7].nueva", RecetaService.FOTO_REPETIDA)));
        verifyNoInteractions(imagenes);
    }

    @Test
    void elVideoTieneQueSerUnEnlaceHttpsAYoutubeOVimeo() {
        assertThat(RecetaService.esEnlaceDeVideo("https://www.youtube.com/watch?v=abc"))
                .isTrue();
        assertThat(RecetaService.esEnlaceDeVideo("https://youtu.be/abc")).isTrue();
        assertThat(RecetaService.esEnlaceDeVideo("https://VIMEO.com/123")).isTrue();
        assertThat(RecetaService.esEnlaceDeVideo("http://www.youtube.com/watch?v=abc"))
                .isFalse();
        assertThat(RecetaService.esEnlaceDeVideo("https://youtube.com.ejemplo.com/x"))
                .isFalse();
        assertThat(RecetaService.esEnlaceDeVideo("https://usuario@youtube.com/x"))
                .isFalse();
        assertThat(RecetaService.esEnlaceDeVideo("javascript:alert(1)")).isFalse();
        assertThat(RecetaService.esEnlaceDeVideo("https://exa mple.com")).isFalse();
    }

    @Test
    void unVideoQueNoEsDeYoutubeNiVimeoDa400YNoSeGuardaNada() {
        prepararValidacionCorrecta(Set.of(3));
        RecetaRequest conVideo = new RecetaRequest(
                "Tortilla",
                "<p>Paso</p>",
                1,
                30,
                4,
                "Fácil",
                List.of(ingrediente(3, "1")),
                null,
                "https://ejemplo.com/v");

        assertThatThrownBy(() -> servicio.crear(conVideo, null, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores()).containsEntry("video", RecetaService.VIDEO_NO_VALIDO));
        verify(recetas, never()).guardarVideo(anyInt(), anyString());
    }

    @Test
    void sobraUnFicheroSiNingunaFotoLoUsaOSiNoSePidenFotos() {
        MockMultipartFile a = ImagenesDePrueba.parteFoto("a.png", ImagenesDePrueba.png());
        MockMultipartFile b = ImagenesDePrueba.parteFoto("b.png", ImagenesDePrueba.png());
        MockMultipartFile vacio = ImagenesDePrueba.parteFoto("vacio.png", new byte[0]);

        assertThatThrownBy(() -> servicio.galeria(List.of(nueva(0, null)), List.of(a, b), List.of()))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores()).containsOnly(entry("fotos", RecetaService.FICHEROS_SOBRANTES)));
        assertThatThrownBy(() -> servicio.galeria(null, List.of(a), List.of()))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores()).containsOnly(entry("fotos", RecetaService.FICHEROS_SOBRANTES)));
        assertThatThrownBy(() -> servicio.galeria(List.of(nueva(0, null)), List.of(vacio), List.of()))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores())
                                .containsOnly(entry("fotos[0].nueva", RecetaService.FICHERO_QUE_FALTA)));
        // Al crear no hay fotos que conservar
        assertThatThrownBy(() -> servicio.galeria(List.of(existente("a.jpg")), null, List.of()))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores())
                                .containsOnly(entry("fotos[0].existente", RecetaService.FOTO_AJENA)));
        assertThat(servicio.galeria(List.of(), null, List.of(new FotoReceta("x.jpg", 0, null))))
                .isEmpty();
        verifyNoInteractions(imagenes);
    }

    @Test
    void crearValidaTipoIngredientesEInstruccionesAntesDeGuardarNada() {
        when(catalogo.existeTipoReceta(1)).thenReturn(false);

        assertThatThrownBy(() ->
                        servicio.crear(peticion("<p> </p>", ingrediente(3, "1"), ingrediente(3, "2")), null, "lucia"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getErrores())
                            .containsEntry("tipo", RecetaService.TIPO_INEXISTENTE)
                            .containsEntry("ingredientes", RecetaService.INGREDIENTES_REPETIDOS)
                            .containsEntry("instrucciones", RecetaService.INSTRUCCIONES_VACIAS);
                });
        verify(recetas, never()).insertar(any(), any(), any(), anyInt(), any(), anyInt(), anyInt(), any());
        verifyNoInteractions(imagenes);
    }

    @Test
    void comoMucho500RecetasPorAutorYNiSiquieraSeMiranLasFotos() {
        // D-089: las eliminadas no cuentan (lo hace la consulta)
        when(recetas.contarDelAutor("lucia")).thenReturn(RecetaService.MAXIMO_RECETAS);

        assertThatThrownBy(() -> servicio.crear(peticion("<p>Paso</p>", ingrediente(3, "1")), null, "lucia"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("LIMITE_RECETAS");
                    assertThat(e.getMessage()).isEqualTo(RecetaService.DEMASIADAS_RECETAS);
                });
        verify(recetas, never()).insertar(any(), any(), any(), anyInt(), any(), anyInt(), anyInt(), any());
        verifyNoInteractions(imagenes, catalogo);
    }

    @Test
    void crearConUnIngredienteQueNoExisteFalla() {
        when(catalogo.existeTipoReceta(1)).thenReturn(true);
        when(recetas.contarIngredientesExistentes(Set.of(3, 999))).thenReturn(1);

        assertThatThrownBy(() -> servicio.crear(
                        peticion("<p>Paso</p>", ingrediente(3, "1"), ingrediente(999, "2")), null, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class,
                        e -> assertThat(e.getErrores())
                                .containsOnly(
                                        java.util.Map.entry("ingredientes", RecetaService.INGREDIENTE_INEXISTENTE)));
    }

    @Test
    void elDetalleDeUnaRecetaNoPublicadaSoloLoVeSuAutor() {
        when(recetas.buscarBase(7, "marcos")).thenReturn(Optional.of(base(7, "lucia", false, null)));
        when(recetas.buscarBase(7, "LUCIA")).thenReturn(Optional.of(base(7, "lucia", false, null)));
        when(recetas.ingredientesDe(7))
                .thenReturn(List.of(
                        new IngredienteRecetaDto(1, "Pollo", "200 g", CARNE),
                        new IngredienteRecetaDto(2, "Tallarines", "300 g", PASTA),
                        new IngredienteRecetaDto(3, "Tomate", "2", HORTALIZAS),
                        new IngredienteRecetaDto(4, "Cebolla", "1", HORTALIZAS)));

        assertThatThrownBy(() -> servicio.detalle(7, "marcos"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));

        RecetaDetalleDto detalle = servicio.detalle(7, "LUCIA");
        assertThat(detalle.esAutor()).isTrue();
        assertThat(detalle.grupos()).containsExactly(CARNE, HORTALIZAS, PASTA);
        assertThat(detalle.maxSemanal()).isEqualTo(3);
    }

    @Test
    void unaRecetaInexistenteDa404() {
        when(recetas.buscarBase(99, "lucia")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.detalle(99, "lucia")).isInstanceOf(ExcepcionApi.class);
    }

    @Test
    void soloElAutorPuedeEditarYPublicar() {
        when(recetas.buscarBase(7, "marcos")).thenReturn(Optional.of(base(7, "lucia", true, null)));

        assertThatThrownBy(() -> servicio.editar(7, peticion("<p>Paso</p>", ingrediente(3, "1")), null, "marcos"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> servicio.publicar(7, false, "marcos"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(recetas, never()).actualizarPublicada(anyInt(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void editarUnaRecetaAjenaNoPublicadaDa404ParaNoRevelarQueExiste() {
        when(recetas.buscarBase(7, "marcos")).thenReturn(Optional.of(base(7, "lucia", false, null)));

        assertThatThrownBy(() -> servicio.editar(7, peticion("<p>Paso</p>", ingrediente(3, "1")), null, "marcos"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void editarSustituyeLosIngredientesYLaGaleriaBorrandoLasFotosQueYaNoEstan() {
        prepararValidacionCorrecta(Set.of(5));
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(base(7, "lucia", true, "vieja.jpg")));
        when(recetas.fotosDe(7))
                .thenReturn(List.of(new FotoReceta("vieja.jpg", 0, null), new FotoReceta("otra.jpg", 1, "Otra")));
        MockMultipartFile fichero = ImagenesDePrueba.parteFoto("nueva.png", ImagenesDePrueba.png());
        when(imagenes.guardar(fichero, "fotos[1]")).thenReturn("nueva.jpg");

        servicio.editar(
                7,
                peticion(List.of(existente("otra.jpg"), nueva(0, "Nueva")), "<p>Paso</p>", ingrediente(5, "1")),
                List.of(fichero),
                "lucia");

        verify(recetas)
                .actualizar(eq(7), eq("Tortilla"), anyString(), eq(1), eq("otra.jpg"), eq(30), eq(4), eq("Fácil"));
        verify(recetas).borrarIngredientes(7);
        verify(recetas).insertarIngredientes(7, List.of(ingrediente(5, "1")));
        InOrder orden = inOrder(recetas);
        orden.verify(recetas).borrarFotos(7);
        // La descripción es la que se pide: sin ella, la foto se queda sin descripción
        orden.verify(recetas)
                .insertarFotos(
                        7, List.of(new FotoReceta("otra.jpg", 0, null), new FotoReceta("nueva.jpg", 1, "Nueva")));
        verify(imagenes).borrarTrasCommit("vieja.jpg");
        verify(imagenes, never()).borrarTrasCommit("otra.jpg");
    }

    @Test
    void editarSinFotosConservaLaGaleria() {
        prepararValidacionCorrecta(Set.of(5));
        // Una receta con imagen y sin filas en foto_receta: su imagen es su única foto
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(base(7, "lucia", true, "vieja.jpg")));

        servicio.editar(7, peticion("<p>Paso</p>", ingrediente(5, "1")), null, "lucia");

        verify(recetas)
                .actualizar(
                        eq(7), anyString(), anyString(), anyInt(), eq("vieja.jpg"), anyInt(), anyInt(), anyString());
        verify(recetas).insertarFotos(7, List.of(new FotoReceta("vieja.jpg", 0, null)));
        verifyNoInteractions(imagenes);
    }

    @Test
    void elAutorPublicaSuReceta() {
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(base(7, "lucia", false, null)));

        servicio.publicar(7, true, "lucia");

        verify(recetas).actualizarPublicada(7, true);
    }

    @Test
    void soloSePuedeDarMeGustaARecetasPublicadas() {
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(base(7, "lucia", false, null)));

        assertThatThrownBy(() -> servicio.darLike(7, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("RECETA_NO_PUBLICADA"));
        verify(recetas, never()).darLike(anyString(), anyInt(), any());
    }

    @Test
    void darYQuitarMeGustaDevuelveElTotal() {
        when(recetas.buscarBase(7, "marcos")).thenReturn(Optional.of(base(7, "lucia", true, null)));
        when(recetas.contarLikes(7)).thenReturn(3L, 2L);

        assertThat(servicio.darLike(7, "marcos")).isEqualTo(new LikesDto(3, true));
        assertThat(servicio.quitarLike(7, "marcos")).isEqualTo(new LikesDto(2, false));
        verify(recetas).darLike("marcos", 7, AHORA);
        verify(recetas).quitarLike("marcos", 7);
    }

    @Test
    void elListadoGeneralSoloIncluyeLasPublicadasYMisRecetasLasDelAutor() {
        FiltroRecetas filtro =
                FiltroRecetas.de(new ParametrosFiltroRecetas(" tortilla ", null, null, null, null, null, null));
        when(recetas.contar(any())).thenReturn(0L);

        Pagina<RecetaResumenDto> pagina = servicio.listar(filtro, "lucia", 0, 9);
        servicio.mias(filtro, "lucia", 0, 9);

        ArgumentCaptor<FiltroRecetas> usados = ArgumentCaptor.forClass(FiltroRecetas.class);
        verify(recetas, org.mockito.Mockito.times(2)).contar(usados.capture());
        assertThat(usados.getAllValues().get(0).soloPublicadas()).isTrue();
        assertThat(usados.getAllValues().get(0).titulo()).isEqualTo("%tortilla%");
        assertThat(usados.getAllValues().get(1).autor()).isEqualTo("lucia");
        assertThat(usados.getAllValues().get(1).soloPublicadas()).isFalse();
        assertThat(pagina.contenido()).isEmpty();
        verify(recetas, never()).buscar(any(), any(), any(), org.mockito.ArgumentMatchers.anyLong(), anyInt());
    }

    @Test
    void lasQueMeGustanSonLasPublicadasConMiMeGustaOrdenadasPorSuFecha() {
        FiltroRecetas filtro =
                FiltroRecetas.de(new ParametrosFiltroRecetas(null, null, null, "Fácil", null, null, null));
        when(recetas.contar(any())).thenReturn(1L);

        servicio.meGustan(filtro, "lucia", 0, 9);

        ArgumentCaptor<FiltroRecetas> usado = ArgumentCaptor.forClass(FiltroRecetas.class);
        verify(recetas).buscar(usado.capture(), eq("lucia"), eq(OrdenRecetas.ME_GUSTA_RECIENTES), eq(0L), eq(9));
        assertThat(usado.getValue().soloPublicadas()).isTrue();
        assertThat(usado.getValue().meGustaDe()).isEqualTo("lucia");
        assertThat(usado.getValue().dificultad()).isEqualTo("Fácil");
    }

    @Test
    void elFiltroQuitaIngredientesRepetidosYNulos() {
        FiltroRecetas filtro = FiltroRecetas.de(new ParametrosFiltroRecetas(
                null, null, null, " ", null, null, new ArrayList<>(Arrays.asList(3, null, 3, 5))));

        assertThat(filtro.ingredientes()).containsExactly(3, 5);
        assertThat(filtro.dificultad()).isNull();
    }

    @Test
    void lasTarjetasPorIdsSeDevuelvenIndexadasYSinIdsNoSeConsulta() {
        RecetaResumenDto tortilla = new RecetaResumenDto(
                1,
                "Tortilla",
                "lucia",
                new TipoRecetaDto(1, "Plato"),
                null,
                30,
                4,
                "Fácil",
                0,
                false,
                null,
                0,
                true,
                null,
                false,
                false);
        when(recetas.buscarPorIds(List.of(1), "lucia")).thenReturn(List.of(tortilla));

        assertThat(servicio.resumenes(List.of(1), "lucia")).containsExactly(java.util.Map.entry(1, tortilla));
        assertThat(servicio.resumenes(List.of(), "lucia")).isEmpty();
    }

    @Test
    void exigirVisibleFallaConRecetasAjenasSinPublicar() {
        when(recetas.buscarBase(7, "marcos")).thenReturn(Optional.of(base(7, "lucia", false, null)));

        assertThatThrownBy(() -> servicio.exigirVisible(7, "marcos"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void eliminarUnaRecetaQueNadieHaConsumidoLaBorraDelTodoConSuImagen() {
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(base(7, "lucia", true, "foto.jpg")));
        when(recetas.tieneConsumos(7)).thenReturn(false);

        servicio.eliminar(7, "lucia");

        InOrder orden = inOrder(recetas);
        orden.verify(recetas).bloquear(7);
        orden.verify(recetas).buscarBase(7, "lucia");
        orden.verify(recetas).borrar(7);
        verify(recetas, never()).marcarEliminada(anyInt());
        verify(imagenes).borrarTrasCommit("foto.jpg");
    }

    @Test
    void eliminarUnaRecetaYaConsumidaSoloLaMarcaParaQueLosConsumosSiganContando() {
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(base(7, "lucia", true, "foto.jpg")));
        when(recetas.tieneConsumos(7)).thenReturn(true);

        servicio.eliminar(7, "lucia");

        verify(recetas).marcarEliminada(7);
        verify(recetas).borrarLikes(7);
        verify(recetas).borrarFotos(7);
        verify(recetas, never()).borrar(anyInt());
        verify(imagenes).borrarTrasCommit("foto.jpg");
    }

    @Test
    void soloElAutorPuedeEliminarSuReceta() {
        when(recetas.buscarBase(7, "marcos")).thenReturn(Optional.of(base(7, "lucia", true, null)));

        assertThatThrownBy(() -> servicio.eliminar(7, "marcos"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(recetas, never()).borrar(anyInt());
        verify(recetas, never()).marcarEliminada(anyInt());
        verifyNoInteractions(imagenes);
    }

    @Test
    void unaRecetaEliminadaNoLaVeNadieNiSiquieraSuAutor() {
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(eliminada(7, "lucia")));

        assertThatThrownBy(() -> servicio.detalle(7, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> servicio.eliminar(7, "lucia")).isInstanceOf(ExcepcionApi.class);
        verify(recetas, never()).borrar(anyInt());
    }

    @Test
    void unaRecetaOcultaSoloLaVenSuAutorYLosAdministradores() {
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(oculta(7, "lucia")));
        when(recetas.buscarBase(7, "marcos")).thenReturn(Optional.of(oculta(7, "lucia")));
        when(recetas.buscarBase(7, "admin")).thenReturn(Optional.of(oculta(7, "lucia")));

        RecetaDetalleDto delAutor = servicio.detalle(7, "lucia");
        assertThat(delAutor.oculta()).isTrue();
        assertThat(delAutor.motivoOculta()).isEqualTo("Contenido inadecuado");
        assertThat(servicio.detalle(7, "admin", true).oculta()).isTrue();
        assertThatThrownBy(() -> servicio.detalle(7, "marcos"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> servicio.detalle(7, "admin", false)).isInstanceOf(ExcepcionApi.class);
    }

    @Test
    void elAutorNoPuedePublicarUnaRecetaOcultaPeroSiDespublicarla() {
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(oculta(7, "lucia")));

        assertThatThrownBy(() -> servicio.publicar(7, true, "lucia")).isInstanceOfSatisfying(ExcepcionApi.class, e -> {
            assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.getCodigo()).isEqualTo("RECETA_OCULTA");
        });
        verify(recetas, never()).actualizarPublicada(7, true);

        servicio.publicar(7, false, "lucia");
        verify(recetas).actualizarPublicada(7, false);
    }

    @Test
    void unaRecetaOcultaNoAdmiteMeGusta() {
        when(recetas.buscarBase(7, "lucia")).thenReturn(Optional.of(oculta(7, "lucia")));

        assertThatThrownBy(() -> servicio.darLike(7, "lucia"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("RECETA_NO_PUBLICADA"));
    }

    @Test
    void ocultarExigeUnMotivoYLoGuardaSinEspacios() {
        when(recetas.buscarBase(7, "admin")).thenReturn(Optional.of(base(7, "lucia", true, null)));

        assertThatThrownBy(() -> servicio.ocultar(7, true, "  ", "admin"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getErrores()).containsOnlyKeys("motivo"));
        verify(recetas, never()).actualizarOculta(anyInt(), org.mockito.ArgumentMatchers.anyBoolean(), any());

        servicio.ocultar(7, true, " Publicidad ", "admin");
        verify(recetas).actualizarOculta(7, true, "Publicidad");
    }

    @Test
    void volverAMostrarUnaRecetaBorraElMotivo() {
        when(recetas.buscarBase(7, "admin")).thenReturn(Optional.of(oculta(7, "lucia")));

        servicio.ocultar(7, false, "da igual", "admin");

        verify(recetas).actualizarOculta(7, false, null);
    }

    @Test
    void noSeOcultaLoQueElAdministradorNoPuedeVer() {
        when(recetas.buscarBase(7, "admin")).thenReturn(Optional.of(base(7, "lucia", false, null)));
        when(recetas.buscarBase(8, "admin")).thenReturn(Optional.of(eliminada(8, "lucia")));

        assertThatThrownBy(() -> servicio.ocultar(7, true, "Spam", "admin")).isInstanceOf(ExcepcionApi.class);
        assertThatThrownBy(() -> servicio.ocultar(8, true, "Spam", "admin")).isInstanceOf(ExcepcionApi.class);
        verify(recetas, never()).actualizarOculta(anyInt(), org.mockito.ArgumentMatchers.anyBoolean(), any());
    }

    @Test
    void lasOcultasUsanSuFiltro() {
        when(recetas.contar(any())).thenReturn(0L);

        servicio.ocultas("admin", 0, 9);

        ArgumentCaptor<FiltroRecetas> usado = ArgumentCaptor.forClass(FiltroRecetas.class);
        verify(recetas).contar(usado.capture());
        assertThat(usado.getValue().soloOcultas()).isTrue();
        assertThat(usado.getValue().soloPublicadas()).isFalse();
    }

    @Test
    void sinTextoNoHaySugerencias() {
        assertThat(servicio.sugerencias(" ")).isEmpty();
        verifyNoInteractions(recetas);
    }

    @Test
    void lasSugerenciasSonComoMuchoOcho() {
        when(recetas.sugerencias("to%", "%to%", 8)).thenReturn(List.of(new SugerenciaRecetaDto(1, "Tortilla")));

        assertThat(servicio.sugerencias("to")).hasSize(1);
    }

    private void prepararValidacionCorrecta(Set<Integer> ingredientes) {
        when(catalogo.existeTipoReceta(1)).thenReturn(true);
        when(recetas.contarIngredientesExistentes(ingredientes)).thenReturn(ingredientes.size());
    }

    private static RecetaRequest peticion(String instrucciones, IngredienteCantidad... ingredientes) {
        return peticion(null, instrucciones, ingredientes);
    }

    private static RecetaRequest peticion(
            List<FotoRequest> fotos, String instrucciones, IngredienteCantidad... ingredientes) {
        return new RecetaRequest(" Tortilla ", instrucciones, 1, 30, 4, "Fácil", List.of(ingredientes), fotos, null);
    }

    private static FotoRequest existente(String nombre) {
        return new FotoRequest(nombre, null, null);
    }

    private static FotoRequest nueva(int fichero, String descripcion) {
        return new FotoRequest(null, fichero, descripcion);
    }

    private static IngredienteCantidad ingrediente(int id, String cantidad) {
        return new IngredienteCantidad(id, cantidad);
    }

    private static RecetaBase base(int id, String autor, boolean publicada, String imagen) {
        return base(id, autor, publicada, false, false, imagen);
    }

    private static RecetaBase eliminada(int id, String autor) {
        return base(id, autor, false, true, false, null);
    }

    private static RecetaBase oculta(int id, String autor) {
        return base(id, autor, true, false, true, null);
    }

    private static RecetaBase base(
            int id, String autor, boolean publicada, boolean eliminada, boolean oculta, String imagen) {
        return new RecetaBase(
                id,
                "Tortilla",
                "<p>Paso</p>",
                autor,
                new TipoRecetaDto(1, "Plato"),
                publicada,
                eliminada,
                oculta,
                oculta ? "Contenido inadecuado" : null,
                imagen,
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
