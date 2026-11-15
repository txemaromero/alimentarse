package es.alimentarse.correo;

import static org.assertj.core.api.Assertions.assertThat;

import es.alimentarse.support.BuzonDePrueba;
import es.alimentarse.support.PropiedadesDePrueba;
import es.alimentarse.usuario.AvisoCuenta;
import es.alimentarse.usuario.ConfirmacionEmailSolicitada;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

/** Con las plantillas reales de {@code mail/}. */
class CorreoServiceTest {

    private final BuzonDePrueba buzon = new BuzonDePrueba();
    private final CorreoService correo =
            new CorreoService(buzon, new CorreoConfig().plantillasCorreo(), PropiedadesDePrueba.crear());

    @Test
    void elCorreoDeBienvenidaSaludaPorElNombreYEnlazaAlLogin() throws Exception {
        correo.enviarBienvenida("lucia@toca.local", "Lucía", null);

        MimeMessage mensaje = buzon.esperarCorreoPara("lucia@toca.local");
        InternetAddress remitente = (InternetAddress) mensaje.getFrom()[0];
        assertThat(remitente.getAddress()).isEqualTo(PropiedadesDePrueba.MAIL_FROM);
        assertThat(remitente.getPersonal()).isEqualTo(CorreoService.REMITENTE);
        assertThat(mensaje.getSubject()).isEqualTo(CorreoService.ASUNTO_BIENVENIDA);
        assertThat(BuzonDePrueba.html(mensaje))
                .contains("Lucía")
                .contains("href=\"" + PropiedadesDePrueba.BASE_URL + "/login\"");
    }

    @Test
    void conElTokenLaBienvenidaPideConfirmarElEmail() throws Exception {
        correo.enviarBienvenida("nueva@toca.local", "Nuria", "abc_DEF-123");

        assertThat(BuzonDePrueba.html(buzon.esperarCorreoPara("nueva@toca.local")))
                .contains("Confirmar mi email")
                .contains(PropiedadesDePrueba.BASE_URL + "/confirmar-email?token=abc_DEF-123")
                .contains("caduca en 24 horas")
                .doesNotContain(">Iniciar sesión<");
    }

    @Test
    void elEnlaceParaConfirmarElEmailDiceSiEsUnCambio() throws Exception {
        correo.enviarConfirmacionEmail(new ConfirmacionEmailSolicitada("Lucía", "nuevo@toca.local", "tok-1", true));
        correo.enviarConfirmacionEmail(new ConfirmacionEmailSolicitada("Carmen", "carmen@toca.local", "tok-2", false));

        MimeMessage cambio = buzon.esperarCorreoPara("nuevo@toca.local");
        assertThat(cambio.getSubject()).isEqualTo(CorreoService.ASUNTO_CONFIRMAR_EMAIL_NUEVO);
        assertThat(BuzonDePrueba.html(cambio))
                .contains("Lucía")
                .contains("sigue con el anterior")
                .contains(PropiedadesDePrueba.BASE_URL + "/confirmar-email?token=tok-1");
        MimeMessage alta = buzon.esperarCorreoPara("carmen@toca.local");
        assertThat(alta.getSubject()).isEqualTo(CorreoService.ASUNTO_CONFIRMAR_EMAIL);
        assertThat(BuzonDePrueba.html(alta))
                .contains("Confirma que este email es tuyo")
                .contains("token=tok-2");
    }

    @Test
    void elCorreoDeRecuperacionEnlazaConElTokenYAvisaDeLaCaducidad() throws Exception {
        correo.enviarRecuperacion("lucia@toca.local", "Lucía", "abc_DEF-123");

        MimeMessage mensaje = buzon.esperarCorreoPara("lucia@toca.local");
        assertThat(mensaje.getSubject()).isEqualTo(CorreoService.ASUNTO_RECUPERACION);
        assertThat(BuzonDePrueba.html(mensaje))
                .contains(PropiedadesDePrueba.BASE_URL + "/restablecer-password?token=abc_DEF-123")
                .contains("caduca en 1 hora");
    }

    @Test
    void elCorreoDeBajaConfirmaQueSeHanBorradoLosDatos() throws Exception {
        correo.enviarBaja("lucia@toca.local", "Lucía");

        MimeMessage mensaje = buzon.esperarCorreoPara("lucia@toca.local");
        assertThat(mensaje.getSubject()).isEqualTo(CorreoService.ASUNTO_BAJA);
        assertThat(BuzonDePrueba.html(mensaje)).contains("Lucía").contains("se ha eliminado");
    }

    @Test
    void losAvisosDeLaCuentaDicenQueHaCambiadoYQueHacerSiNoHaSidoElUsuario() throws Exception {
        correo.enviarAvisoCuenta(
                new AvisoCuenta("Lucía", "antes@toca.local", AvisoCuenta.Motivo.EMAIL_CAMBIADO, "despues@toca.local"));
        correo.enviarAvisoCuenta(
                new AvisoCuenta("Lucía", "google@toca.local", AvisoCuenta.Motivo.GOOGLE_VINCULADO, null));
        correo.enviarAvisoCuenta(
                new AvisoCuenta("Lucía", "sin@toca.local", AvisoCuenta.Motivo.GOOGLE_DESVINCULADO, null));

        MimeMessage email = buzon.esperarCorreoPara("antes@toca.local");
        assertThat(email.getSubject()).isEqualTo(CorreoService.ASUNTO_EMAIL_CAMBIADO);
        assertThat(BuzonDePrueba.html(email))
                .contains("Lucía")
                .contains("despues@toca.local")
                .contains("Si no has sido tú")
                .contains("href=\"" + PropiedadesDePrueba.BASE_URL + "/login\"");
        assertThat(buzon.esperarCorreoPara("google@toca.local").getSubject())
                .isEqualTo(CorreoService.ASUNTO_GOOGLE_VINCULADO);
        assertThat(BuzonDePrueba.html(buzon.esperarCorreoPara("sin@toca.local")))
                .contains("ya no está vinculada con Google");
    }

    @Test
    void losDatosDelUsuarioSeEscapanEnElHtml() {
        correo.enviarBienvenida("x@toca.local", "<script>alert(1)</script>", null);

        assertThat(BuzonDePrueba.html(buzon.esperarCorreoPara("x@toca.local")))
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
    }
}
