package es.alimentarse.push;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Cifrado de los avisos push (D-078), comprobado con el ejemplo del apéndice A de RFC 8291. */
class CifradoPushTest {

    // Valores de RFC 8291, apéndice A
    static final String MENSAJE = "When I grow up, I want to be a watermelon";
    static final String SERVIDOR_PRIVADA = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
    static final String SERVIDOR_PUBLICA =
            "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    static final String NAVEGADOR_PRIVADA = "q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94";
    static final String NAVEGADOR_PUBLICA =
            "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    static final String AUTH = "BTBZMqHH6r4Tts7J_aSIgg";
    static final String SAL = "DGv6ra1nlYgDCS1FRnbzlw";
    static final String CUERPO =
            "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";

    @Test
    void reproduceElEjemploDeRfc8291() throws Exception {
        KeyPair servidor = new KeyPair(
                CifradoPush.clavePublica(CifradoPush.base64Url(SERVIDOR_PUBLICA)),
                CifradoPush.clavePrivada(CifradoPush.base64Url(SERVIDOR_PRIVADA)));

        byte[] cuerpo = CifradoPush.cifrar(
                MENSAJE.getBytes(UTF_8),
                CifradoPush.base64Url(NAVEGADOR_PUBLICA),
                CifradoPush.base64Url(AUTH),
                servidor,
                CifradoPush.base64Url(SAL));

        assertThat(CifradoPush.base64Url(cuerpo)).isEqualTo(CUERPO);
    }

    @Test
    void conClavesYSalNuevasElNavegadorLoDescifra() throws Exception {
        byte[] cuerpo = CifradoPush.cifrar(
                "{\"notification\":{}}".getBytes(UTF_8),
                CifradoPush.base64Url(NAVEGADOR_PUBLICA),
                CifradoPush.base64Url(AUTH));
        byte[] otro = CifradoPush.cifrar(
                "{\"notification\":{}}".getBytes(UTF_8),
                CifradoPush.base64Url(NAVEGADOR_PUBLICA),
                CifradoPush.base64Url(AUTH));

        assertThat(cuerpo).isNotEqualTo(otro);
        assertThat(new String(DescifradoNavegador.descifrar(cuerpo, NAVEGADOR_PRIVADA, NAVEGADOR_PUBLICA, AUTH), UTF_8))
                .isEqualTo("{\"notification\":{}}");
    }

    @Test
    void laClavePublicaVaComoPuntoSinComprimir() throws Exception {
        byte[] punto = CifradoPush.base64Url(SERVIDOR_PUBLICA);
        assertThat(CifradoPush.puntoSinComprimir(CifradoPush.clavePublica(punto)))
                .isEqualTo(punto);
    }

    @Test
    void reconoceLosPuntosDeLaCurvaP256() {
        assertThat(CifradoPush.puntoP256(CifradoPush.base64Url(NAVEGADOR_PUBLICA)))
                .isTrue();
        assertThat(CifradoPush.puntoP256(CifradoPush.base64Url(SERVIDOR_PUBLICA)))
                .isTrue();

        byte[] fueraDeLaCurva = CifradoPush.base64Url(NAVEGADOR_PUBLICA);
        fueraDeLaCurva[64] ^= 1;
        assertThat(CifradoPush.puntoP256(fueraDeLaCurva)).isFalse();

        byte[] mayorQueP = new byte[65];
        Arrays.fill(mayorQueP, (byte) 0xFF);
        mayorQueP[0] = 4;
        assertThat(CifradoPush.puntoP256(mayorQueP)).isFalse();

        byte[] comprimido = Arrays.copyOf(CifradoPush.base64Url(NAVEGADOR_PUBLICA), 65);
        comprimido[0] = 2;
        assertThat(CifradoPush.puntoP256(comprimido)).isFalse();
        assertThat(CifradoPush.puntoP256(new byte[33])).isFalse();
    }

    @Test
    void rechazaClavesConOtroFormatoYMensajesQueNoCabenEnUnRegistro() {
        assertThatThrownBy(() -> CifradoPush.clavePublica(new byte[33])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CifradoPush.clavePrivada(new byte[31])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CifradoPush.cifrar(
                        new byte[CifradoPush.MAXIMO_MENSAJE + 1],
                        CifradoPush.base64Url(NAVEGADOR_PUBLICA),
                        CifradoPush.base64Url(AUTH)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
