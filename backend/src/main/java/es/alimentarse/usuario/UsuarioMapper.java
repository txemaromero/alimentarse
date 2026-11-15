package es.alimentarse.usuario;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jspecify.annotations.Nullable;

/** Acceso a {@code usuarios} y {@code usuario_rol}. SQL en {@code mybatis/UsuarioMapper.xml}. */
@Mapper
public interface UsuarioMapper {

    Optional<Usuario> buscarPorId(String id);

    Optional<Usuario> buscarPorEmail(String email);

    Optional<Usuario> buscarPorTokenPassword(String hashToken);

    /** El usuario vinculado a esa cuenta de Google (D-085). */
    Optional<Usuario> buscarPorGoogleId(String googleId);

    /** Vincula la cuenta de Google si el usuario no tiene ya una: devuelve 1 si lo hace y 0 si no (D-086). */
    int vincularGoogle(@Param("id") String id, @Param("googleId") String googleId);

    int desvincularGoogle(String id);

    int anularRecuperacion(String id);

    int guardarTokenEmail(
            @Param("id") String id,
            @Param("hashToken") String hashToken,
            @Param("expiracion") Instant expiracion,
            @Param("emailPendiente") @Nullable String emailPendiente);

    /** El usuario con ese enlace de confirmación del email, si no ha caducado (D-088). */
    Optional<Usuario> buscarPorTokenEmail(@Param("hashToken") String hashToken, @Param("ahora") Instant ahora);

    int confirmarEmail(String id);

    int cancelarCambioEmail(String id);

    int marcarEmailVerificado(String id);

    /** ¿Está bloqueado? Vacío si el usuario no existe (D-063). */
    /** Vacío si la cuenta no existe. */
    Optional<EstadoSesion> estadoSesion(String id);

    int versionSesion(String id);

    /** Las sesiones abiertas dejan de valer (D-070). */
    int anularSesiones(String id);

    int actualizarBloqueado(@Param("id") String id, @Param("bloqueado") boolean bloqueado);

    boolean existeId(String id);

    /** ¿Hay al menos un usuario con el rol ADMIN? */
    boolean hayAdministradores();

    /** ¿Usa otro usuario este email? {@code exceptoId} excluye al propio usuario (puede ser {@code null}). */
    boolean existeEmail(@Param("email") String email, @Param("exceptoId") String exceptoId);

    void insertar(Usuario usuario);

    void asignarRol(@Param("usuario") String usuario, @Param("rol") String rol);

    int quitarRol(@Param("usuario") String usuario, @Param("rol") String rol);

    List<String> rolesDe(String id);

    int actualizarPerfil(
            @Param("id") String id,
            @Param("nombre") String nombre,
            @Param("apellido1") String apellido1,
            @Param("apellido2") String apellido2,
            @Param("email") String email);

    /** Cambia la contraseña e invalida cualquier token de recuperación pendiente. */
    int actualizarPassword(@Param("id") String id, @Param("password") String passwordCifrada);

    /**
     * La misma contraseña cifrada con otro coste (D-090): sin tocar la versión de sesión ni el token de recuperación, y
     * solo si el hash sigue siendo {@code anterior}.
     */
    int rehacerHashPassword(@Param("id") String id, @Param("anterior") String anterior, @Param("nuevo") String nuevo);

    int guardarTokenPassword(
            @Param("id") String id, @Param("hashToken") String hashToken, @Param("expiracion") Instant expiracion);
}
