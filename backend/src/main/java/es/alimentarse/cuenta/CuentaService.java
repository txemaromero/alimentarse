package es.alimentarse.cuenta;

import es.alimentarse.admin.AdminMapper;
import es.alimentarse.comun.error.ExcepcionApi;
import es.alimentarse.cuenta.CuentaMapper.FilaActividad;
import es.alimentarse.cuenta.CuentaMapper.FilaIngrediente;
import es.alimentarse.imagen.ImagenService;
import es.alimentarse.usuario.PerfilDto;
import es.alimentarse.usuario.Usuario;
import es.alimentarse.usuario.UsuarioMapper;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derechos sobre los datos personales (D-064): descargarlos y darse de baja. */
@Service
public class CuentaService {

    static final String PASSWORD_INCORRECTA = "La contraseña no es correcta";
    static final String ULTIMO_ADMINISTRADOR = "Eres el único administrador: nombra a otro antes de darte de baja.";

    private final CuentaMapper cuenta;
    private final UsuarioMapper usuarios;
    private final AdminMapper admin;
    private final ImagenService imagenes;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public CuentaService(
            CuentaMapper cuenta,
            UsuarioMapper usuarios,
            AdminMapper admin,
            ImagenService imagenes,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventos,
            Clock reloj) {
        this.cuenta = cuenta;
        this.usuarios = usuarios;
        this.admin = admin;
        this.imagenes = imagenes;
        this.passwordEncoder = passwordEncoder;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** Perfil, recetas (con ingredientes), consumos, «me gusta», planificador, valoraciones y avisos del usuario. */
    @Transactional(readOnly = true)
    public DatosPersonalesDto exportar(String yo) {
        Usuario usuario = buscar(yo);
        Map<Integer, List<DatosPersonalesDto.Ingrediente>> ingredientes = cuenta.ingredientesDe(yo).stream()
                .collect(Collectors.groupingBy(
                        FilaIngrediente::receta,
                        Collectors.mapping(
                                i -> new DatosPersonalesDto.Ingrediente(i.nombre(), i.cantidad(), i.grupo()),
                                Collectors.toList())));
        Map<Integer, List<String>> fotos = cuenta.fotosDe(yo).stream()
                .collect(Collectors.groupingBy(
                        CuentaMapper.FilaFoto::receta,
                        Collectors.mapping(f -> ImagenService.url(f.nombre()), Collectors.toList())));
        List<DatosPersonalesDto.Receta> recetas = cuenta.recetasDe(yo).stream()
                .map(r -> new DatosPersonalesDto.Receta(
                        r.id(),
                        r.titulo(),
                        r.tipo(),
                        r.instrucciones(),
                        r.tiempo(),
                        r.comensales(),
                        r.dificultad(),
                        r.publicada(),
                        r.oculta(),
                        r.motivoOculta(),
                        r.fechaCreacion(),
                        ImagenService.url(r.imagen()),
                        fotos.getOrDefault(r.id(), List.of()),
                        r.video(),
                        ingredientes.getOrDefault(r.id(), List.of())))
                .toList();
        return new DatosPersonalesDto(
                reloj.instant(),
                PerfilDto.de(usuario, usuarios.rolesDe(yo)),
                recetas,
                actividad(cuenta.consumosDe(yo)),
                actividad(cuenta.meGustaDe(yo)),
                actividad(cuenta.planDe(yo)),
                cuenta.valoracionesDe(yo).stream()
                        .map(v -> new DatosPersonalesDto.Valoracion(
                                v.fecha(), v.receta(), v.titulo(), v.nota(), v.comentario(), v.oculta()))
                        .toList(),
                cuenta.suscripcionesDe(yo).stream()
                        .map(s -> new DatosPersonalesDto.Aviso(
                                s.fecha(), java.net.URI.create(s.endpoint()).getHost()))
                        .toList());
    }

    /**
     * Borra la cuenta con sus consumos, sus «me gusta», sus valoraciones, sus avisos, su planificador y sus recetas. Las recetas que otros han consumido se conservan
     * eliminadas y sin autor, para que esos consumos sigan contando (D-061). Exige la contraseña y que no sea el último
     * administrador. Tras el commit se borran las imágenes y se envía un correo de confirmación.
     */
    @Transactional
    public void darDeBaja(String yo, String password) {
        // Bloqueos antes de cualquier lectura: ni un cambio de rol ni un consumo nuevo se cruzan con la baja
        admin.bloquearRolAdmin();
        cuenta.bloquearRecetasDe(yo);
        Usuario usuario = buscar(yo);
        if (!passwordEncoder.matches(password, usuario.password())) {
            throw ExcepcionApi.peticionIncorrecta(
                    "PASSWORD_INCORRECTA", PASSWORD_INCORRECTA + ".", Map.of("password", PASSWORD_INCORRECTA));
        }
        if (admin.esAdmin(yo) && admin.contarAdmins() <= 1) {
            throw ExcepcionApi.conflicto("ULTIMO_ADMINISTRADOR", ULTIMO_ADMINISTRADOR, null);
        }
        List<String> imagenesDeSusRecetas = cuenta.imagenesDe(yo);
        cuenta.borrarConsumosDe(yo);
        cuenta.borrarPlanDe(yo);
        cuenta.borrarLikesDe(yo);
        cuenta.borrarLikesDeSusRecetas(yo);
        cuenta.borrarValoracionesDe(yo);
        cuenta.borrarSuscripcionesDe(yo);
        cuenta.borrarFotosDe(yo);
        cuenta.anonimizarRecetasConsumidas(yo);
        cuenta.borrarRecetasDe(yo);
        cuenta.borrarRolesDe(yo);
        cuenta.borrarUsuario(yo);
        imagenesDeSusRecetas.forEach(imagenes::borrarTrasCommit);
        eventos.publishEvent(new CuentaEliminada(usuario.nombre(), usuario.email()));
    }

    private Usuario buscar(String id) {
        return usuarios.buscarPorId(id).orElseThrow(() -> ExcepcionApi.noEncontrado("El usuario no existe."));
    }

    private static List<DatosPersonalesDto.Actividad> actividad(List<FilaActividad> filas) {
        return filas.stream()
                .map(f -> new DatosPersonalesDto.Actividad(f.fecha(), f.receta(), f.titulo()))
                .toList();
    }
}
