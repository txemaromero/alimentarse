package es.alimentarse.admin;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Consultas de la gestión de usuarios. SQL en {@code mybatis/AdminMapper.xml}. */
@Mapper
public interface AdminMapper {

    /**
     * @param patron patrón {@code LIKE} escapado sobre id, nombre, apellidos o email; {@code null} = todos
     */
    List<UsuarioAdminDto> buscar(
            @Param("patron") String patron, @Param("desplazamiento") long desplazamiento, @Param("limite") int limite);

    long contar(@Param("patron") String patron);

    Optional<UsuarioAdminDto> buscarPorId(String id);

    boolean esAdmin(String id);

    long contarAdmins();

    /**
     * Bloquea la fila del rol ADMIN hasta el final de la transacción: los cambios de rol de administrador se ejecutan
     * de uno en uno (D-043). Debe ser la primera consulta de la transacción.
     *
     * @return id del rol ADMIN
     */
    int bloquearRolAdmin();
}
