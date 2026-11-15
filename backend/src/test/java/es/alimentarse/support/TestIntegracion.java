package es.alimentarse.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de integración con el contexto completo, MockMvc y una base de datos MySQL real, recién migrada. Usa
 * Testcontainers si hay Docker y, si no, la base de datos de {@code TEST_DB_URL} (D-016). Los correos van a un
 * {@link BuzonDePrueba} y los tokens se crean con {@link TokensDePrueba}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({BaseDatosTestConfig.class, ApoyoTestConfig.class})
public @interface TestIntegracion {}
