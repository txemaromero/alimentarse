package es.alimentarse.auth;

/**
 * La vinculación con Google pendiente (D-086).
 *
 * @param email el de Google, que es también el de la cuenta de Toca que se va a vincular
 */
public record DatosVinculacionGoogleDto(String email) {}
