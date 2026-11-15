import { LoginResponse, Rol } from '../app/core/modelos';

/** Respuesta del login para los tests. El JWT va en una cookie que el frontend no ve (D-070). */
export function loginDePrueba(
  roles: Rol[] = ['USER'],
  expiraEnSegundos = 3600,
  nombre = 'Lucía',
): LoginResponse {
  return {
    caduca: new Date(Date.now() + expiraEnSegundos * 1000).toISOString(),
    usuario: { id: 'lucia', nombre, roles, emailVerificado: true },
  };
}
