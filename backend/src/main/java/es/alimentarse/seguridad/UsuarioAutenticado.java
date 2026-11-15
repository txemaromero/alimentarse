package es.alimentarse.seguridad;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Usuario de la petición actual, reconstruido a partir del JWT. Los controladores lo reciben con
 * {@code @AuthenticationPrincipal UsuarioAutenticado yo}.
 *
 * @param roles nombres de rol tal cual están en la tabla {@code roles}: {@code USER}, {@code ADMIN}
 */
public record UsuarioAutenticado(String id, String nombre, List<String> roles) {

    public static final String ROL_USER = "USER";
    public static final String ROL_ADMIN = "ADMIN";

    public UsuarioAutenticado {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public boolean esAdmin() {
        return roles.contains(ROL_ADMIN);
    }

    public List<GrantedAuthority> autoridades() {
        return roles.stream()
                .<GrantedAuthority>map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
    }
}
