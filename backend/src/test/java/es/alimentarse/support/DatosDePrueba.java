package es.alimentarse.support;

import java.time.Duration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Crea datos de prueba por SQL dentro de la transacción del test (que se deshace al terminar). Las fechas se calculan
 * con {@code NOW()} de la base de datos, en UTC como la aplicación.
 */
public class DatosDePrueba {

    private final JdbcTemplate jdbc;

    public DatosDePrueba(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Usuario con rol USER, como los que se registran. */
    public void usuario(String id) {
        jdbc.update(
                "INSERT INTO usuarios (ID, NOMBRE, APELLIDO1, EMAIL, PASSWORD) VALUES (?, ?, 'Prueba', ?, 'x')",
                id,
                id,
                id + "@pruebas.local");
        jdbc.update("INSERT INTO usuario_rol (USUARIO, ROL) SELECT ?, ID FROM roles WHERE NOMBRE = 'USER'", id);
    }

    /** Usuario con roles USER y ADMIN. */
    public void admin(String id) {
        usuario(id);
        jdbc.update("INSERT INTO usuario_rol (USUARIO, ROL) SELECT ?, ID FROM roles WHERE NOMBRE = 'ADMIN'", id);
    }

    /** Receta de tipo Plato con los ingredientes indicados por nombre (cantidad «1»). */
    public int receta(String titulo, String autor, boolean publicada, int haceDias, String... ingredientes) {
        jdbc.update(
                "INSERT INTO recetas (TITULO, INSTRUCCIONES, USUARIO, TIPO, PUBLICADA, TIEMPO, COMENSALES, DIFICULTAD,"
                        + " FECHA_CREACION) SELECT ?, '<p>Pasos</p>', ?, ID, ?, 10, 2, 'Fácil', NOW() - INTERVAL ? DAY"
                        + " FROM tipo_receta WHERE NOMBRE = 'Plato'",
                titulo,
                autor,
                publicada,
                haceDias);
        int id = ultimoId();
        for (String ingrediente : ingredientes) {
            int filas = jdbc.update(
                    "INSERT INTO ingrediente_receta (INGREDIENTE, RECETA, CANTIDAD)"
                            + " SELECT ID, ?, '1' FROM ingredientes WHERE NOMBRE = ?",
                    id,
                    ingrediente);
            if (filas != 1) {
                throw new IllegalArgumentException("No existe el ingrediente " + ingrediente);
            }
        }
        return id;
    }

    /** Comida planificada para dentro del tiempo indicado (D-068). */
    public void comidaPlanificada(String usuario, int receta, Duration dentroDe) {
        jdbc.update(
                "INSERT INTO usuario_receta_planificada (USUARIO, RECETA, FECHA)"
                        + " VALUES (?, ?, NOW() + INTERVAL ? SECOND)",
                usuario,
                receta,
                dentroDe.toSeconds());
    }

    /** Consumo de la receta hace el tiempo indicado. */
    public long consumo(String usuario, int receta, Duration hace) {
        jdbc.update(
                "INSERT INTO usuario_receta_consumida (USUARIO, RECETA, FECHA) VALUES (?, ?, NOW() - INTERVAL ? SECOND)",
                usuario,
                receta,
                hace.toSeconds());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void like(String usuario, int receta) {
        jdbc.update("INSERT INTO like_receta (USUARIO, RECETA) VALUES (?, ?)", usuario, receta);
    }

    /** «Me gusta» dado hace el tiempo indicado. */
    public void like(String usuario, int receta, Duration hace) {
        jdbc.update(
                "INSERT INTO like_receta (USUARIO, RECETA, FECHA) VALUES (?, ?, NOW() - INTERVAL ? SECOND)",
                usuario,
                receta,
                hace.toSeconds());
    }

    public int idIngrediente(String nombre) {
        return jdbc.queryForObject("SELECT ID FROM ingredientes WHERE NOMBRE = ?", Integer.class, nombre);
    }

    private int ultimoId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }
}
