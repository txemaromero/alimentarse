package es.alimentarse.support;

import es.alimentarse.config.AppProperties;
import java.time.Duration;
import java.util.Base64;

/** {@link AppProperties} para tests unitarios, sin contexto de Spring. */
public final class PropiedadesDePrueba {

    /** 48 bytes en Base64: clave HS384 válida. */
    public static final String SECRETO_JWT =
            Base64.getEncoder().encodeToString("clave-de-pruebas-unitarias-de-alimentarse-48b..".getBytes());

    public static final String BASE_URL = "https://toca.example";
    public static final String MAIL_FROM = "no-reply@toca.local";

    private PropiedadesDePrueba() {}

    public static AppProperties crear() {
        return conJwt(SECRETO_JWT, 8);
    }

    public static AppProperties conJwt(String secreto, int expiracionHoras) {
        return new AppProperties(
                BASE_URL,
                "./uploads",
                MAIL_FROM,
                new AppProperties.Jwt(secreto, expiracionHoras, false),
                new AppProperties.AdminInicial("admin-1234", "admin@toca.local"),
                new AppProperties.LimiteIntentos(
                        new AppProperties.Limite(10, Duration.ofMinutes(1)),
                        new AppProperties.Limite(5, Duration.ofMinutes(15)),
                        new AppProperties.Limite(10, Duration.ofHours(1)),
                        new AppProperties.Limite(20, Duration.ofMinutes(1)),
                        new AppProperties.Limite(20, Duration.ofMinutes(15))));
    }

    public static AppProperties conUploadDir(String uploadDir) {
        AppProperties base = crear();
        return new AppProperties(
                base.baseUrl(), uploadDir, base.mailFrom(), base.jwt(), base.adminInicial(), base.limiteIntentos());
    }

    public static AppProperties conAdminInicial(String password, String email) {
        AppProperties base = crear();
        return new AppProperties(
                base.baseUrl(),
                base.uploadDir(),
                base.mailFrom(),
                base.jwt(),
                new AppProperties.AdminInicial(password, email),
                base.limiteIntentos());
    }
}
