package es.alimentarse.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.apache.catalina.util.NetMaskSet;
import org.apache.catalina.valves.RemoteIpValve;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

/**
 * Proxies de confianza del perfil {@code prod} (D-090): solo de ellos se cree {@code X-Forwarded-For}, y de esa IP
 * salen los límites de intentos. Se comprueba con {@link NetMaskSet}, lo que usa Tomcat cuando el valor son rangos CIDR.
 */
class ProxiesDeConfianzaTest {

    private static final String PROPIEDAD = "server.tomcat.remoteip.internal-proxies";

    @Test
    void porDefectoSoloSeFiaDeEstaMaquina() throws Exception {
        MockEnvironment entorno = perfilProd();
        NetMaskSet proxies = proxies(entorno);

        assertThat(entorno.getProperty("server.forward-headers-strategy")).isEqualTo("native");
        assertThat(proxies.contains("127.0.0.1")).isTrue();
        assertThat(proxies.contains("::1")).isTrue();
    }

    @Test
    void porDefectoNoSeFiaDeLasRedesPrivadas() throws Exception {
        // La LAN, la pasarela de Docker, la red de un PaaS: desde ellas, un cliente podía inventarse su IP
        NetMaskSet proxies = proxies(perfilProd());

        for (String ip :
                List.of("10.0.0.5", "172.17.0.1", "192.168.1.20", "100.64.0.1", "169.254.1.1", "fd00::1", "fe80::1")) {
            assertThat(proxies.contains(ip)).as(ip).isFalse();
        }
    }

    @Test
    void elProxyDeOtraMaquinaSeIndicaConLaVariable() throws Exception {
        MockEnvironment entorno = perfilProd();
        entorno.setProperty("PROXIES_DE_CONFIANZA", "10.0.0.0/8, 192.168.1.5/32");

        NetMaskSet proxies = proxies(entorno);

        assertThat(proxies.contains("10.1.2.3")).isTrue();
        assertThat(proxies.contains("192.168.1.5")).isTrue();
        assertThat(proxies.contains("192.168.1.6")).isFalse();
        assertThat(proxies.contains("127.0.0.1")).isFalse();
    }

    @Test
    void vaciaNoSeFiaDeNinguno() throws Exception {
        // Así llega desde Docker Compose, que no tiene proxy delante
        MockEnvironment entorno = perfilProd();
        entorno.setProperty("PROXIES_DE_CONFIANZA", "");
        RemoteIpValve valvula = new RemoteIpValve();

        valvula.setInternalProxies(entorno.getProperty(PROPIEDAD));

        assertThat(valvula.getInternalProxies()).isNull();
    }

    private static MockEnvironment perfilProd() throws IOException {
        MockEnvironment entorno = new MockEnvironment();
        new YamlPropertySourceLoader()
                .load("application-prod", new ClassPathResource("application-prod.yml"))
                .forEach(entorno.getPropertySources()::addLast);
        return entorno;
    }

    private static NetMaskSet proxies(MockEnvironment entorno) {
        String valor = entorno.getProperty(PROPIEDAD);
        // Con «/», Tomcat lo lee como rangos CIDR; sin ella, como una expresión regular
        assertThat(valor).contains("/");
        return NetMaskSet.parse(valor);
    }
}
