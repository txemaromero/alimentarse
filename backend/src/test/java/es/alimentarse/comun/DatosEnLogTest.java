package es.alimentarse.comun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El log sin datos personales (D-089). */
class DatosEnLogTest {

    @Test
    void enmascaraElEmailDejandoLaInicialYElDominio() {
        assertThat(DatosEnLog.email(" lucia@toca.local ")).isEqualTo("l***@toca.local");
        assertThat(DatosEnLog.email("sin-arroba")).isEqualTo("***");
        assertThat(DatosEnLog.email("@toca.local")).isEqualTo("***");
        assertThat(DatosEnLog.email(null)).isEqualTo("(sin email)");
        assertThat(DatosEnLog.email(" ")).isEqualTo("(sin email)");
    }

    @Test
    void quitaLosEmailsDeLosMensajesDelServidorDeCorreo() {
        String mensaje = "jakarta.mail.SendFailedException: Invalid Addresses;\n"
                + "  nested exception is: 550 5.1.1 <marcos.gil@ejemplo.es>: Recipient address rejected"
                + " (también 'otra@ejemplo.org')";

        assertThat(DatosEnLog.sinEmails(mensaje))
                .contains("<m***@ejemplo.es>", "'o***@ejemplo.org'", "Recipient address rejected")
                .doesNotContain("marcos.gil", "otra@");
        assertThat(DatosEnLog.sinEmails("Connection refused")).isEqualTo("Connection refused");
        assertThat(DatosEnLog.sinEmails(null)).isEmpty();
    }

    @Test
    void deUnErrorDeMysqlSoloDejaLaRestriccion() {
        assertThat(DatosEnLog.restriccion("Duplicate entry 'lucia@toca.local' for key 'usuarios.UK_USUARIOS_EMAIL'"))
                .isEqualTo("usuarios.UK_USUARIOS_EMAIL");
        assertThat(DatosEnLog.restriccion("Cannot add or update a child row: a foreign key constraint fails"
                        + " (`toca`.`like_receta`, CONSTRAINT `FK_LIKE_RECETA` FOREIGN KEY (`RECETA`)"
                        + " REFERENCES `recetas` (`ID`))"))
                .isEqualTo("FK_LIKE_RECETA");
        assertThat(DatosEnLog.restriccion("Check constraint 'CK_VALORACION_NOTA' is violated."))
                .isEqualTo("CK_VALORACION_NOTA");
        assertThat(DatosEnLog.restriccion("Data too long for column 'EMAIL' at row 1: lucia@toca.local"))
                .isEqualTo("(restricción desconocida)");
        assertThat(DatosEnLog.restriccion(null)).isEqualTo("(restricción desconocida)");
    }
}
