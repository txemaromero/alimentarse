package es.alimentarse.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;

class ServidorWebConfigTest {

    @Test
    void elManifestDeLaPwaSeSirveComoApplicationManifestJson() {
        ConfigurableServletWebServerFactory fabrica = mock(ConfigurableServletWebServerFactory.class);

        new ServidorWebConfig().tiposMimePwa().customize(fabrica);

        ArgumentCaptor<MimeMappings> tipos = ArgumentCaptor.forClass(MimeMappings.class);
        verify(fabrica).addMimeMappings(tipos.capture());
        assertThat(tipos.getValue().get("webmanifest")).isEqualTo("application/manifest+json");
    }
}
