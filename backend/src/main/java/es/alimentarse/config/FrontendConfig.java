package es.alimentarse.config;

import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sirve el build de Angular que va dentro del JAR (12).
 *
 * <ul>
 *   <li>Cualquier ruta que no empiece por {@code /api} y no sea un fichero devuelve {@code index.html}: así funcionan
 *       las rutas de Angular al recargar o al abrir un enlace ({@code /recetas/5}).
 *   <li>Los ficheros con hash en el nombre ({@code main-ABC123.js}, {@code styles-XYZ.css}, {@code media/}) se cachean un
 *       año: si cambian, cambia su nombre.
 *   <li>El resto ({@code index.html}, {@code ngsw.json}, los scripts del service worker, el manifest, los iconos) no
 *       llevan caché propia, así que se aplica la de Spring Security ({@code no-store}): un despliegue nuevo se ve en
 *       la siguiente carga y el service worker decide qué guarda.
 * </ul>
 *
 * Sustituye a la configuración de recursos estáticos de Spring Boot ({@code spring.web.resources.add-mappings: false}).
 */
@Configuration(proxyBeanMethods = false)
public class FrontendConfig implements WebMvcConfigurer {

    /** Scripts que no llevan hash en el nombre aunque estén en la raíz. */
    static final String[] SIN_HASH = {"/ngsw-worker.js", "/safety-worker.js", "/worker-basic.min.js"};

    private final String ubicacion;

    public FrontendConfig(@Value("${app.frontend.ubicacion:classpath:/static/}") String ubicacion) {
        this.ubicacion = ubicacion;
    }

    /**
     * La raíz no llega a los resolvers de recursos (una ruta vacía se descarta antes), y la página de bienvenida de
     * Spring Boot solo existe si hay un {@code index.html} en su carpeta estática por defecto. Se reenvía siempre.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registro) {
        registro.addViewController("/").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler(SIN_HASH).addResourceLocations(ubicacion);
        registro.addResourceHandler("/*.js", "/*.css", "/media/**")
                .addResourceLocations(ubicacion)
                .setCacheControl(
                        CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        registro.addResourceHandler("/**")
                .addResourceLocations(ubicacion)
                .resourceChain(false)
                .addResolver(new RutasDeAngular());
    }

    /** Busca el fichero pedido y, si no existe y es una ruta de la aplicación, devuelve {@code index.html}. */
    static class RutasDeAngular extends PathResourceResolver {

        @Override
        protected Resource getResource(String ruta, Resource ubicacion) throws IOException {
            Resource recurso = super.getResource(ruta, ubicacion);
            if (recurso != null) {
                return recurso;
            }
            return esRutaDeAngular(ruta) ? super.getResource("index.html", ubicacion) : null;
        }

        /** No es de la API ni parece un fichero (el último segmento no tiene extensión). */
        static boolean esRutaDeAngular(String ruta) {
            if (ruta.equals("api") || ruta.startsWith("api/")) {
                return false;
            }
            String ultimo = ruta.substring(ruta.lastIndexOf('/') + 1);
            return !ultimo.contains(".");
        }
    }
}
