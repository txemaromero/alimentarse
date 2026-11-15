package es.alimentarse.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.seguridad.JwtService;
import es.alimentarse.seguridad.UsuarioAutenticado;
import es.alimentarse.support.PropiedadesDePrueba;
import es.alimentarse.usuario.AvisoCuenta;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Qué pasa al volver de Google (D-085) y la vinculación con la contraseña (D-086). */
class AccesoGoogleServiceTest {

    private static final CuentaGoogle ANA =
            new CuentaGoogle("google-ana", " ana@gmail.com ", true, "Ana", "García López");
    private static final SesionIniciada SESION = new SesionIniciada(
            "jwt",
            new LoginResponse(Instant.EPOCH, new LoginResponse.UsuarioSesion("ana", "Ana", List.of("USER"), true)));

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final String cifrada = passwordEncoder.encode("clave-de-ana");
    private UsuarioMapper usuarios;
    private AuthService auth;
    private JwtService jwt;
    private ApplicationEventPublisher eventos;
    private AccesoGoogleService servicio;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioMapper.class);
        auth = mock(AuthService.class);
        eventos = mock(ApplicationEventPublisher.class);
        jwt = new JwtService(PropiedadesDePrueba.crear(), Clock.systemUTC());
        servicio = new AccesoGoogleService(usuarios, auth, jwt, passwordEncoder, eventos);
        when(auth.sesion(any())).thenReturn(SESION);
    }

    @Test
    void conLaCuentaDeGoogleYaVinculadaEntra() {
        Usuario ana = usuario("ana", false, "google-ana");
        when(usuarios.buscarPorGoogleId("google-ana")).thenReturn(Optional.of(ana));

        assertThat(servicio.acceder(ANA)).isEqualTo(new AccesoGoogleService.Entra(SESION));
        verify(auth).sesion(ana);
        verify(usuarios, never()).vincularGoogle(anyString(), anyString());
    }

    @Test
    void unaCuentaBloqueadaNoEntraTampocoConGoogle() {
        when(usuarios.buscarPorGoogleId("google-ana")).thenReturn(Optional.of(usuario("ana", true, "google-ana")));

        assertThat(servicio.acceder(ANA)).isEqualTo(new AccesoGoogleService.Rechazado(AccesoGoogleService.BLOQUEADA));
        verify(auth, never()).sesion(any());
    }

    @Test
    void conElMismoEmailVerificadoPideLaContrasenaAntesDeVincular() {
        when(usuarios.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario("ana", false, null)));

        AccesoGoogleService.Resultado resultado = servicio.acceder(ANA);

        assertThat(resultado).isInstanceOf(AccesoGoogleService.Vincular.class);
        String token = ((AccesoGoogleService.Vincular) resultado).token();
        assertThat(jwt.validarVinculacionGoogle(token))
                .contains(new JwtService.VinculacionGoogle("google-ana", "ana", "ana@gmail.com"));
        assertThat(servicio.vinculacionPendiente(token)).isEqualTo(new DatosVinculacionGoogleDto("ana@gmail.com"));
        // Ni entra ni se vincula todavía, y el token no vale como sesión ni como registro
        verify(usuarios, never()).vincularGoogle(anyString(), anyString());
        verify(auth, never()).sesion(any());
        assertThat(jwt.validar(token)).isEmpty();
        assertThat(jwt.validarRegistroGoogle(token)).isEmpty();
    }

    /** Con el email confirmado en Toca y verificado por Google, es la misma persona: no hace falta la contraseña. */
    @Test
    void conElMismoEmailConfirmadoSeVinculaSinLaContrasenaYAvisa() {
        Usuario ana = confirmada("ana");
        when(usuarios.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(ana));
        when(usuarios.vincularGoogle("ana", "google-ana")).thenReturn(1);

        assertThat(servicio.acceder(ANA)).isEqualTo(new AccesoGoogleService.Entra(SESION));
        verify(eventos)
                .publishEvent(new AvisoCuenta("Ana", "ana@gmail.com", AvisoCuenta.Motivo.GOOGLE_VINCULADO, null));
        verify(auth).sesion(ana);
    }

    @Test
    void siOtraPeticionVinculaAntesLaCuentaConfirmadaNoEntra() {
        when(usuarios.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(confirmada("ana")));
        when(usuarios.vincularGoogle("ana", "google-ana")).thenReturn(0);

        assertThat(servicio.acceder(ANA))
                .isEqualTo(new AccesoGoogleService.Rechazado(AccesoGoogleService.OTRA_CUENTA_DE_GOOGLE));
        verify(auth, never()).sesion(any());
    }

    @Test
    void conElMismoEmailPeroBloqueadaOVinculadaAOtraCuentaDeGoogleNoEntra() {
        when(usuarios.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario("ana", true, null)));
        assertThat(servicio.acceder(ANA)).isEqualTo(new AccesoGoogleService.Rechazado(AccesoGoogleService.BLOQUEADA));

        when(usuarios.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(usuario("ana", false, "google-otra")));
        assertThat(servicio.acceder(ANA))
                .isEqualTo(new AccesoGoogleService.Rechazado(AccesoGoogleService.OTRA_CUENTA_DE_GOOGLE));
    }

    @Test
    void unEmailQueSoloCoincideSinAcentosNoEsElMismo() {
        // La colación de MySQL los da por iguales, pero son direcciones distintas
        Usuario otra =
                new Usuario("otra", "Ana", "G", null, "anà@gmail.com", cifrada, null, null, false, null, true, null);
        when(usuarios.buscarPorEmail("ana@gmail.com")).thenReturn(Optional.of(otra));

        assertThat(servicio.acceder(ANA)).isInstanceOf(AccesoGoogleService.Registro.class);
    }

    @Test
    void sinEmailVerificadoNiSeVinculaNiSeRegistra() {
        CuentaGoogle sinVerificar = new CuentaGoogle("google-x", "ana@gmail.com", false, "Ana", "");
        CuentaGoogle sinEmail = new CuentaGoogle("google-y", "", true, "Ana", "");

        assertThat(servicio.acceder(sinVerificar))
                .isEqualTo(new AccesoGoogleService.Rechazado(AccesoGoogleService.EMAIL_NO_VERIFICADO));
        assertThat(servicio.acceder(sinEmail))
                .isEqualTo(new AccesoGoogleService.Rechazado(AccesoGoogleService.EMAIL_NO_VERIFICADO));
        verify(usuarios, never()).buscarPorEmail(anyString());
    }

    @Test
    void conLaContrasenaSeVinculaAvisaYEntra() {
        Usuario ana = usuario("ana", false, null);
        when(usuarios.buscarPorId("ana")).thenReturn(Optional.of(ana));
        when(usuarios.vincularGoogle("ana", "google-ana")).thenReturn(1);

        assertThat(servicio.vincular(vinculacion(), "clave-de-ana")).isEqualTo(SESION);

        verify(usuarios).vincularGoogle("ana", "google-ana");
        // Sabe la contraseña y Google ha verificado ese email: queda confirmado (D-088)
        verify(usuarios).marcarEmailVerificado("ana");
        verify(eventos)
                .publishEvent(new AvisoCuenta("Ana", "ana@gmail.com", AvisoCuenta.Motivo.GOOGLE_VINCULADO, null));
        verify(auth).sesion(ana);
    }

    @Test
    void conOtraContrasenaNoSeVincula() {
        when(usuarios.buscarPorId("ana")).thenReturn(Optional.of(usuario("ana", false, null)));

        assertThatThrownBy(() -> servicio.vincular(vinculacion(), "otra"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCodigo()).isEqualTo("PASSWORD_INCORRECTA");
                    assertThat(e.getErrores()).containsOnlyKeys("password");
                });
        verify(usuarios, never()).vincularGoogle(anyString(), anyString());
        verify(auth, never()).sesion(any());
    }

    @Test
    void unaCuentaBloqueadaNoSeVinculaAunqueSepaLaContrasena() {
        when(usuarios.buscarPorId("ana")).thenReturn(Optional.of(usuario("ana", true, null)));

        assertThatThrownBy(() -> servicio.vincular(vinculacion(), "clave-de-ana"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getCodigo()).isEqualTo("CUENTA_BLOQUEADA");
                });
        verify(usuarios, never()).vincularGoogle(anyString(), anyString());
    }

    @Test
    void siYaEstabaVinculadaConEsaCuentaEntraYConOtraNoLaSustituye() {
        when(usuarios.buscarPorId("ana")).thenReturn(Optional.of(usuario("ana", false, "google-ana")));
        assertThat(servicio.vincular(vinculacion(), "clave-de-ana")).isEqualTo(SESION);

        when(usuarios.buscarPorId("ana")).thenReturn(Optional.of(usuario("ana", false, "google-otra")));
        assertThatThrownBy(() -> servicio.vincular(vinculacion(), "clave-de-ana"))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("YA_VINCULADA");
                });

        verify(usuarios, never()).vincularGoogle(anyString(), anyString());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void siOtraPeticionLaVinculaAntesNoSeSustituye() {
        when(usuarios.buscarPorId("ana")).thenReturn(Optional.of(usuario("ana", false, null)));
        when(usuarios.vincularGoogle("ana", "google-ana")).thenReturn(0);

        assertThatThrownBy(() -> servicio.vincular(vinculacion(), "clave-de-ana"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT));
        verify(eventos, never()).publishEvent(any(Object.class));
        verify(auth, never()).sesion(any());
    }

    @Test
    void sinVinculacionPendienteOSinLaCuentaDa404() {
        String deRegistro = jwt.emitirRegistroGoogle(new JwtService.RegistroGoogle("g", "a@gmail.com", "Ana", ""));
        for (String token : new String[] {null, "no-es-un-token", deRegistro}) {
            assertThatThrownBy(() -> servicio.vinculacionPendiente(token))
                    .isInstanceOfSatisfying(
                            ExcepcionApi.class,
                            e -> assertThat(e.getCodigo()).isEqualTo("VINCULACION_GOOGLE_CADUCADA"));
        }
        // La cuenta se ha borrado mientras tanto
        when(usuarios.buscarPorId("ana")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.vincular(vinculacion(), "clave-de-ana"))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void alguienNuevoEmpiezaUnRegistroConLoQueDaGoogle() {
        AccesoGoogleService.Resultado resultado = servicio.acceder(ANA);

        assertThat(resultado).isInstanceOf(AccesoGoogleService.Registro.class);
        String token = ((AccesoGoogleService.Registro) resultado).token();
        assertThat(servicio.pendiente(token))
                .isEqualTo(new DatosRegistroGoogleDto("ana@gmail.com", "Ana", "García", "López"));
        // El token del registro no es una sesión ni una vinculación
        assertThat(jwt.validar(token)).isEmpty();
        assertThat(jwt.validarVinculacionGoogle(token)).isEmpty();
    }

    @Test
    void sinApellidosElFormularioLosDejaVacios() {
        String token = jwt.emitirRegistroGoogle(new JwtService.RegistroGoogle("g", "a@gmail.com", "Ana", ""));
        assertThat(servicio.pendiente(token)).isEqualTo(new DatosRegistroGoogleDto("a@gmail.com", "Ana", "", null));
    }

    @Test
    void sinRegistroPendienteOConUnTokenQueNoValeDa404() {
        for (String token : new String[] {
            null,
            "no-es-un-token",
            jwt.emitir(new UsuarioAutenticado("ana", "Ana", List.of("USER")), 1).valor()
        }) {
            assertThatThrownBy(() -> servicio.pendiente(token)).isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                assertThat(e.getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(e.getCodigo()).isEqualTo("REGISTRO_GOOGLE_CADUCADO");
            });
        }
    }

    @Test
    void completarElRegistroCreaLaCuentaVinculadaConElEmailDeGoogleYEntra() {
        String token = jwt.emitirRegistroGoogle(
                new JwtService.RegistroGoogle("google-ana", "ana@gmail.com", "Ana", "García López"));
        Usuario creada = usuario("anag", false, "google-ana");
        when(auth.passwordAleatoria()).thenReturn("hash-aleatorio");
        when(auth.crearCuenta("anag", "Ana", "García", "López", "ana@gmail.com", "hash-aleatorio", "google-ana"))
                .thenReturn(creada);

        assertThat(servicio.completarRegistro(token, new RegistroGoogleRequest("anag", "Ana", "García", "López")))
                .isEqualTo(SESION);
        verify(auth).sesion(creada);
    }

    @Test
    void unRegistroYaCompletadoNoSirveParaEntrarOtraVez() {
        String token = jwt.emitirRegistroGoogle(new JwtService.RegistroGoogle("google-ana", "ana@gmail.com", "", ""));
        when(usuarios.buscarPorGoogleId("google-ana")).thenReturn(Optional.of(usuario("ana", false, "google-ana")));

        assertThatThrownBy(() -> servicio.completarRegistro(token, new RegistroGoogleRequest("otra", "Ana", "G", null)))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("REGISTRO_GOOGLE_CADUCADO"));
        verify(auth, never()).crearCuenta(any(), any(), any(), any(), any(), any(), any());
        verify(auth, never()).sesion(any());
    }

    private String vinculacion() {
        return jwt.emitirVinculacionGoogle(new JwtService.VinculacionGoogle("google-ana", "ana", "ana@gmail.com"));
    }

    private Usuario usuario(String id, boolean bloqueado, String googleId) {
        return new Usuario(
                id, "Ana", "García", null, id + "@gmail.com", cifrada, null, null, bloqueado, googleId, false, null);
    }

    private Usuario confirmada(String id) {
        return new Usuario(id, "Ana", "García", null, id + "@gmail.com", cifrada, null, null, false, null, true, null);
    }
}
