package es.alimentarse.auth;

import es.alimentarse.config.AppProperties;
import es.alimentarse.config.GoogleProperties;
import es.alimentarse.seguridad.FiltroLimiteIntentos;
import es.alimentarse.seguridad.LimiteIntentosService;
import es.alimentarse.seguridad.RespuestasErrorSeguridad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Acceso con Google (D-085): flujo de código de autorización de OpenID Connect con PKCE, solo si hay credenciales. Va
 * en su propia cadena de filtros, la de {@code /oauth2/**} y {@code /login/oauth2/**}, que usa la sesión HTTP solo
 * durante el intercambio con Google; el resto de la aplicación sigue sin sesión (D-070).
 */
@Configuration
@Conditional(AccesoGoogleConfig.ConCredenciales.class)
class AccesoGoogleConfig {

    static final String REGISTRO = "google";

    @Bean
    ClientRegistrationRepository registrosOAuth(GoogleProperties google) {
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE
                .getBuilder(REGISTRO)
                .clientId(google.clientId())
                .clientSecret(google.clientSecret())
                .scope("openid", "email", "profile")
                .build());
    }

    @Bean
    @Order(1)
    SecurityFilterChain cadenaGoogle(
            HttpSecurity http,
            ClientRegistrationRepository registros,
            ManejadorAccesoGoogle manejador,
            LimiteIntentosService limiteIntentos,
            AppProperties propiedades,
            RespuestasErrorSeguridad respuestas)
            throws Exception {
        DefaultOAuth2AuthorizationRequestResolver peticiones = new DefaultOAuth2AuthorizationRequestResolver(
                registros, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        peticiones.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll())
                // La protección frente a CSRF del intercambio es el parámetro «state», que comprueba Spring Security
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .oauth2Login(login -> login.loginPage("/login")
                        .authorizationEndpoint(autorizacion -> autorizacion.authorizationRequestResolver(peticiones))
                        .successHandler(manejador)
                        .failureHandler(manejador))
                // Cada ida a Google crea una sesión HTTP: con límite por IP (D-086)
                .addFilterBefore(
                        new FiltroLimiteIntentos(limiteIntentos, propiedades, respuestas),
                        OAuth2AuthorizationRequestRedirectFilter.class)
                .build();
    }

    /** Solo con el ID de cliente y el secreto. */
    static class ConCredenciales implements Condition {
        @Override
        public boolean matches(ConditionContext contexto, AnnotatedTypeMetadata metadatos) {
            String id = contexto.getEnvironment().getProperty("app.google.client-id");
            String secreto = contexto.getEnvironment().getProperty("app.google.client-secret");
            return new GoogleProperties(id, secreto).habilitado();
        }
    }
}
