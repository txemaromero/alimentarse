package es.alimentarse.seguridad;

import es.alimentarse.config.AppProperties;
import es.alimentarse.config.ContrasenasProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.util.WebUtils;

/**
 * Seguridad sin estado con JWT, en una cookie {@code HttpOnly} o en la cabecera {@code Authorization}. Toda la API exige sesión salvo la autenticación, las imágenes y la documentación (que
 * solo existe en dev); todo lo que no es {@code /api} es el frontend y es público.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Content-Security-Policy (D-060). Scripts solo del propio origen, sin excepciones: el build de Angular no lleva
     * scripts en línea ({@code inlineCritical: false}). Los estilos en línea hacen falta porque Angular, ng-bootstrap y
     * Font Awesome los insertan. {@code data:} y {@code blob:} en imágenes: iconos SVG de Bootstrap y la vista previa de
     * la imagen de una receta.
     */
    static final String POLITICA_CSP = String.join(
            "; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: blob:",
            "font-src 'self' data:",
            "connect-src 'self'",
            "worker-src 'self'",
            "manifest-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    /** Funciones del navegador que la aplicación no usa. */
    static final String POLITICA_PERMISOS = "camera=(), microphone=(), geolocation=(), payment=(), usb=()";

    @Bean
    SecurityFilterChain cadenaDeSeguridad(
            HttpSecurity http,
            JwtService jwt,
            EstadoCuentas cuentas,
            LimiteIntentosService limiteIntentos,
            AppProperties propiedades,
            RespuestasErrorSeguridad respuestas)
            throws Exception {
        return http.csrf(csrf -> csrf.spa()
                        .requireCsrfProtectionMatcher(SecurityConfig::exigeCsrf)
                        // Sin esto, cada petición con JWT cuenta como un inicio de sesión y el token cambia en cada
                        // respuesta: dos peticiones simultáneas podrían acabar en un 403
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas.requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/imagenes/**", "/api/health", "/api/health/**")
                        .permitAll()
                        // Swagger UI (solo dev): /api/docs redirige a /api/swagger-ui/index.html
                        .requestMatchers("/api/docs/**", "/api/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasRole(UsuarioAutenticado.ROL_ADMIN)
                        .requestMatchers("/api/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .headers(cabeceras -> cabeceras
                        .contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_CSP))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permisos -> permisos.policy(POLITICA_PERMISOS))
                        .crossOriginOpenerPolicy(coop -> coop.policy(CrossOriginOpenerPolicy.SAME_ORIGIN)))
                .exceptionHandling(
                        errores -> errores.authenticationEntryPoint(respuestas).accessDeniedHandler(respuestas))
                // Mismo punto de inserción: se ejecutan en este orden
                .addFilterBefore(
                        new FiltroLimiteIntentos(limiteIntentos, propiedades, respuestas),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new FiltroJwt(jwt, cuentas), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Solo las escrituras que se autentican con la cookie (D-070): con {@code Authorization} no hay riesgo, porque el
     * navegador no añade esa cabecera por su cuenta, y sin sesión no hay nada que proteger (responderán 401).
     */
    static boolean esBearer(@Nullable String cabecera) {
        return cabecera != null && cabecera.startsWith(FiltroJwt.PREFIJO);
    }

    static boolean exigeCsrf(HttpServletRequest peticion) {
        return CsrfFilter.DEFAULT_CSRF_MATCHER.matches(peticion)
                && !peticion.getRequestURI().startsWith("/api/auth/")
                // Solo un token Bearer evita la cookie (FiltroJwt); otra cabecera, que podría poner el navegador, no
                && !esBearer(peticion.getHeader(HttpHeaders.AUTHORIZATION))
                && WebUtils.getCookie(peticion, CookieSesion.NOMBRE) != null;
    }

    @Bean
    PasswordEncoder passwordEncoder(ContrasenasProperties contrasenas, Environment entorno) {
        return new BCryptPasswordEncoder(CifradoPassword.coste(contrasenas.costeBcrypt(), entorno));
    }

    /** Tras un login correcto con un hash de menos coste, el proveedor lo rehace y lo guarda en {@code usuarios}. */
    @Bean
    AuthenticationManager authenticationManager(UsuarioDetallesService usuarios, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuarios);
        proveedor.setPasswordEncoder(passwordEncoder);
        proveedor.setUserDetailsPasswordService(usuarios);
        return new ProviderManager(proveedor);
    }
}
