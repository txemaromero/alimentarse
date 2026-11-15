import { Injectable, computed, inject, signal } from '@angular/core';
import { borrarDatosGuardados } from './datos-sin-conexion';
import { EstadoListados } from './estado-listados';
import { LoginResponse, UsuarioSesion } from './modelos';

interface SesionGuardada {
  caduca: string;
  usuario: UsuarioSesion;
}

export const CLAVE_SESION = 'alimentarse.sesion';

/**
 * Sesión del usuario en el frontend: sus datos básicos y cuándo caduca, en localStorage. El token de verdad va en una
 * cookie `HttpOnly` que el JavaScript no puede leer (D-070); esto solo sirve para pintar la interfaz. Al arrancar se
 * descarta una sesión caducada.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly listados = inject(EstadoListados);
  private readonly estado = signal<SesionGuardada | null>(leer());

  readonly usuario = computed(() => this.estado()?.usuario ?? null);
  readonly autenticado = computed(() => this.estado() !== null);
  readonly esAdmin = computed(() => this.usuario()?.roles.includes('ADMIN') ?? false);

  iniciar(respuesta: LoginResponse): void {
    const sesion: SesionGuardada = { caduca: respuesta.caduca, usuario: respuesta.usuario };
    escribir(sesion);
    this.listados.vaciar();
    void borrarDatosGuardados();
    this.estado.set(sesion);
  }

  /** Tras editar el perfil: actualiza el nombre que se muestra. */
  actualizarNombre(nombre: string): void {
    const sesion = this.estado();
    if (sesion) {
      const nueva = { ...sesion, usuario: { ...sesion.usuario, nombre } };
      escribir(nueva);
      this.estado.set(nueva);
    }
  }

  /** Tras confirmar el email, o al saber que ya lo está (D-088): deja de mostrarse el aviso. */
  actualizarEmailVerificado(emailVerificado: boolean): void {
    const sesion = this.estado();
    if (sesion && sesion.usuario.emailVerificado !== emailVerificado) {
      const nueva = { ...sesion, usuario: { ...sesion.usuario, emailVerificado } };
      escribir(nueva);
      this.estado.set(nueva);
    }
  }

  cerrar(): void {
    escribir(null);
    this.listados.vaciar();
    // Los datos guardados para usar sin conexión no deben quedar a la vista de quien use después el equipo
    void borrarDatosGuardados();
    this.estado.set(null);
  }

  /** ¿Hay sesión y sigue sin caducar? Si ha caducado, la cierra. */
  vigente(ahora = Date.now()): boolean {
    const sesion = this.estado();
    if (sesion && caducada(sesion, ahora)) {
      this.cerrar();
      return false;
    }
    return sesion !== null;
  }
}

function caducada(sesion: SesionGuardada, ahora: number): boolean {
  const caducidad = new Date(sesion.caduca).getTime();
  return Number.isNaN(caducidad) || caducidad <= ahora;
}

function leer(): SesionGuardada | null {
  try {
    const texto = localStorage.getItem(CLAVE_SESION);
    if (!texto) {
      return null;
    }
    const sesion = JSON.parse(texto) as SesionGuardada;
    // Sin `caduca` es una sesión anterior a D-070, con el token en localStorage: se descarta
    if (!sesion?.caduca || !sesion.usuario || caducada(sesion, Date.now())) {
      localStorage.removeItem(CLAVE_SESION);
      return null;
    }
    return sesion;
  } catch {
    return null;
  }
}

function escribir(sesion: SesionGuardada | null): void {
  try {
    if (sesion) {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    } else {
      localStorage.removeItem(CLAVE_SESION);
    }
  } catch {
    // Sin almacenamiento (modo privado estricto): la sesión dura lo que la pestaña
  }
}
