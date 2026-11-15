package es.alimentarse.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.config.AppProperties;
import es.alimentarse.seguridad.JwtService;
import es.alimentarse.seguridad.LimiteIntentosService;
import es.alimentarse.seguridad.UsuarioAutenticado;
import es.alimentarse.support.PropiedadesDePrueba;
import es.alimentarse.usuario.ConfirmacionEmailService;
import es.alimentarse.usuario.PerfilDto;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-01T10:00:00Z");

    @Mock
    UsuarioMapper usuarios;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    ApplicationEventPublisher eventos;

    @Mock
    ConfirmacionEmailService confirmaciones;

    @Mock
    LimiteIntentosService limite;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final AppProperties propiedades = PropiedadesDePrueba.crear();
    private final JwtService jwt = new JwtService(propiedades, reloj);
    private AuthService auth;

    @BeforeEach
    void crearServicio() {
        auth = new AuthService(
                usuarios,
                passwordEncoder,
                authenticationManager,
                jwt,
                eventos,
                reloj,
                confirmaciones,
                limite,
                propiedades);
        // Ninguna cuenta ha agotado sus fallos, salvo en los tests que lo digan
        lenient().when(limite.agotado(anyString(), any())).thenReturn(Duration.ZERO);
    }

    /** Agotados los fallos de un usuario (D-090), 429 sin comprobar la contraseña. */
    @Test
    void conLosFallosAgotadosNoSeCompruebaLaContrasena() {
        AppProperties.Limite fallos = propiedades.limiteIntentos().loginCuenta();
        when(limite.agotado("login-cuenta:lucia", fallos)).thenReturn(Duration.ofMinutes(7));

        assertThatThrownBy(() -> auth.login(new LoginRequest(" lucia ", "demo1234")))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getCodigo()).isEqualTo("DEMASIADOS_INTENTOS");
                    assertThat(e).hasMessage(AuthService.DEMASIADOS_FALLOS);
                    assertThat(e.getReintentarTras()).isEqualTo(Duration.ofMinutes(7));
                });
        verifyNoInteractions(authenticationManager);
        verify(limite, never()).registrarFallo(anyString(), any());
    }

    @Test
    void unaContrasenaIncorrectaCuentaUnFalloDeLoEscrito() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("no"));

        assertThatThrownBy(() -> auth.login(new LoginRequest("Lucia@Toca.local", "otra")))
                .isInstanceOf(ExcepcionApi.class);

        verify(limite)
                .registrarFallo(
                        "login-cuenta:Lucia@Toca.local",
                        propiedades.limiteIntentos().loginCuenta());
    }

    @Test
    void unaContrasenaDeMasDe72BytesTambienCuentaComoFallo() {
        assertThatThrownBy(() -> auth.login(new LoginRequest("lucia", "ñ".repeat(40))))
                .isInstanceOf(ExcepcionApi.class);

        verifyNoInteractions(authenticationManager);
        verify(limite)
                .registrarFallo(
                        "login-cuenta:lucia", propiedades.limiteIntentos().loginCuenta());
    }

    @Test
    void registrarCreaElUsuarioConRolUserContrasenaCifradaYAvisaParaElCorreo() {
        when(confirmaciones.prepararEnlace("lucia", null)).thenReturn("token-confirmar");
        PerfilDto perfil = auth.registrar(
                new RegistroRequest(" lucia ", " Lucía ", "Fernández", "  ", "lucia@toca.local", "demo1234"));

        ArgumentCaptor<Usuario> insertado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).insertar(insertado.capture());
        assertThat(insertado.getValue().id()).isEqualTo("lucia");
        assertThat(insertado.getValue().nombre()).isEqualTo("Lucía");
        assertThat(insertado.getValue().apellido2()).isNull();
        assertThat(passwordEncoder.matches("demo1234", insertado.getValue().password()))
                .isTrue();
        verify(usuarios).asignarRol("lucia", "USER");
        // El correo de bienvenida lleva el enlace para confirmar el email (D-088)
        verify(eventos).publishEvent(new UsuarioRegistrado("lucia", "Lucía", "lucia@toca.local", "token-confirmar"));
        verify(usuarios, never()).marcarEmailVerificado(anyString());
        assertThat(perfil.roles()).containsExactly("USER");
    }

    @Test
    void registrarConIdYEmailOcupadosDevuelveAmbosErrores() {
        when(usuarios.existeId("lucia")).thenReturn(true);
        when(usuarios.existeEmail("lucia@toca.local", null)).thenReturn(true);

        assertThatThrownBy(() ->
                        auth.registrar(new RegistroRequest("lucia", "L", "F", null, "lucia@toca.local", "demo1234")))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getErrores())
                            .containsEntry("id", AuthService.ID_EN_USO)
                            .containsEntry("email", AuthService.EMAIL_EN_USO);
                });
        verify(usuarios, never()).insertar(any());
    }

    @Test
    void registrarConSoloElEmailOcupadoSoloMarcaElEmail() {
        when(usuarios.existeEmail("lucia@toca.local", null)).thenReturn(true);

        assertThatThrownBy(() ->
                        auth.registrar(new RegistroRequest("otra", "L", "F", null, "lucia@toca.local", "demo1234")))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getErrores()).containsOnlyKeys("email"));
    }

    @Test
    void loginDevuelveUnTokenConLosRolesDelUsuario() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated("lucia", null, List.of()));
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(usuario("lucia", null, null)));
        when(usuarios.rolesDe("lucia")).thenReturn(List.of("USER", "ADMIN"));

        when(usuarios.versionSesion("lucia")).thenReturn(3);

        SesionIniciada sesion = auth.login(new LoginRequest("lucia@toca.local", "demo1234"));

        assertThat(sesion.respuesta().usuario())
                .isEqualTo(new LoginResponse.UsuarioSesion("lucia", "Lucía", List.of("USER", "ADMIN"), false));
        assertThat(jwt.validar(sesion.token())).hasValueSatisfying(valida -> {
            assertThat(valida.usuario()).isEqualTo(new UsuarioAutenticado("lucia", "Lucía", List.of("USER", "ADMIN")));
            assertThat(valida.version()).isEqualTo(3);
            assertThat(valida.caduca()).isEqualTo(sesion.respuesta().caduca());
        });
    }

    @Test
    void unUsuarioBloqueadoNoPuedeIniciarSesionAunqueSepaLaContrasena() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated("lucia", null, List.of()));
        Usuario bloqueada = new Usuario(
                "lucia", "Lucía", "Fernández", null, "l@toca.local", "x", null, null, true, null, false, null);
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(bloqueada));

        assertThatThrownBy(() -> auth.login(new LoginRequest("lucia", "demo1234")))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getCodigo()).isEqualTo("CUENTA_BLOQUEADA");
                });
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mal"));

        assertThatThrownBy(() -> auth.login(new LoginRequest("lucia", "incorrecta")))
                .isInstanceOfSatisfying(ExcepcionApi.class, e -> {
                    assertThat(e.getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getCodigo()).isEqualTo("CREDENCIALES_INVALIDAS");
                });
    }

    @Test
    void solicitarRecuperacionConEmailDesconocidoNoHaceNada() {
        when(usuarios.buscarPorEmail("nadie@toca.local")).thenReturn(Optional.empty());

        auth.solicitarRecuperacion("nadie@toca.local");

        verify(usuarios, never()).guardarTokenPassword(anyString(), anyString(), any());
        verifyNoInteractions(eventos);
    }

    @Test
    void solicitarRecuperacionGuardaElHashDelTokenConCaducidadDeUnaHora() {
        when(usuarios.buscarPorEmail("lucia@toca.local")).thenReturn(Optional.of(usuario("lucia", null, null)));

        auth.solicitarRecuperacion("lucia@toca.local");

        ArgumentCaptor<RecuperacionPasswordSolicitada> evento =
                ArgumentCaptor.forClass(RecuperacionPasswordSolicitada.class);
        verify(eventos).publishEvent(evento.capture());
        String token = evento.getValue().token();
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        verify(usuarios).guardarTokenPassword("lucia", AuthService.sha256(token), AHORA.plusSeconds(3600));
    }

    @Test
    void restablecerConTokenValidoCambiaLaContrasena() {
        when(usuarios.buscarPorTokenPassword(AuthService.sha256("token-bueno")))
                .thenReturn(Optional.of(usuario("lucia", AuthService.sha256("token-bueno"), AHORA.plusSeconds(60))));

        auth.restablecer(new RestablecerPasswordRequest("token-bueno", "nueva-clave"));

        ArgumentCaptor<String> cifrada = ArgumentCaptor.forClass(String.class);
        verify(usuarios).actualizarPassword(eq("lucia"), cifrada.capture());
        assertThat(passwordEncoder.matches("nueva-clave", cifrada.getValue())).isTrue();
        // El enlace llegó a su email: queda confirmado (D-088)
        verify(usuarios).marcarEmailVerificado("lucia");
        // Y puede entrar ya, aunque alguien le hubiera agotado los fallos (D-090)
        verify(limite).olvidar("login-cuenta:lucia");
        verify(limite).olvidar("login-cuenta:lucia@toca.local");
    }

    @Test
    void restablecerConTokenCaducadoFalla() {
        when(usuarios.buscarPorTokenPassword(AuthService.sha256("token-viejo")))
                .thenReturn(Optional.of(usuario("lucia", AuthService.sha256("token-viejo"), AHORA.minusSeconds(1))));

        assertThatThrownBy(() -> auth.restablecer(new RestablecerPasswordRequest("token-viejo", "nueva-clave")))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("TOKEN_INVALIDO"));
        verify(usuarios, never()).actualizarPassword(anyString(), anyString());
    }

    @Test
    void restablecerConTokenDesconocidoOYaUsadoFalla() {
        when(usuarios.buscarPorTokenPassword(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auth.restablecer(new RestablecerPasswordRequest("usado", "nueva-clave")))
                .isInstanceOfSatisfying(
                        ExcepcionApi.class, e -> assertThat(e.getEstado()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void elHashDelTokenEsSha256EnHexadecimal() {
        assertThat(AuthService.sha256("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private static Usuario usuario(String id, String tokenPassword, Instant tokenExpiracion) {
        return new Usuario(
                id,
                "Lucía",
                "Fernández",
                null,
                id + "@toca.local",
                "x",
                tokenPassword,
                tokenExpiracion,
                false,
                null,
                false,
                null);
    }

    @Test
    void unaCuentaCreadaConGoogleQuedaVinculadaYAvisaParaElCorreo() {
        when(usuarios.existeId("anag")).thenReturn(false);
        when(usuarios.existeEmail("ana@gmail.com", null)).thenReturn(false);

        Usuario creada = auth.crearCuenta(" anag ", " Ana ", "García", " ", " ana@gmail.com ", "hash", "google-ana");

        assertThat(creada.id()).isEqualTo("anag");
        assertThat(creada.apellido2()).isNull();
        verify(usuarios).insertar(creada);
        verify(usuarios).vincularGoogle("anag", "google-ana");
        // Google ya ha verificado el email: sin enlace de confirmación (D-088)
        verify(usuarios).marcarEmailVerificado("anag");
        verify(confirmaciones, never()).prepararEnlace(any(), any());
        verify(eventos).publishEvent(new UsuarioRegistrado("anag", "Ana", "ana@gmail.com", null));
    }

    @Test
    void laContrasenaDeUnaCuentaDeGoogleEsAleatoriaYCifrada() {
        String una = auth.passwordAleatoria();
        String otra = auth.passwordAleatoria();

        assertThat(una).startsWith("$2").isNotEqualTo(otra);
    }

    @Test
    void losDatosDeLaSesionSalenDelTokenSiElFiltroLoHaAceptado() {
        UsuarioAutenticado lucia = new UsuarioAutenticado("lucia", "Lucía", List.of("USER"));
        JwtService.TokenEmitido token = jwt.emitir(lucia, 1);
        Usuario confirmada = new Usuario(
                "lucia", "Lucía", "Fernández", null, "l@toca.local", "x", null, null, false, null, true, null);
        when(usuarios.buscarPorId("lucia")).thenReturn(Optional.of(confirmada));

        assertThat(auth.datosSesion(token.valor(), lucia))
                .isEqualTo(new LoginResponse(
                        token.caduca(), new LoginResponse.UsuarioSesion("lucia", "Lucía", List.of("USER"), true)));
        for (Runnable sinSesion : List.<Runnable>of(
                () -> auth.datosSesion(token.valor(), null),
                () -> auth.datosSesion(null, lucia),
                () -> auth.datosSesion(token.valor(), new UsuarioAutenticado("otro", "Otro", List.of("USER"))))) {
            assertThatThrownBy(sinSesion::run)
                    .isInstanceOfSatisfying(
                            ExcepcionApi.class, e -> assertThat(e.getCodigo()).isEqualTo("SIN_SESION"));
        }
    }
}
