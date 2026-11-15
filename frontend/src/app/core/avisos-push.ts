import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { SwPush } from '@angular/service-worker';
import { NEVER, Observable, firstValueFrom, take } from 'rxjs';
import { type Esquemas, PruebaPush } from './modelos';

/** De quién es la suscripción de este navegador, para no heredarla al cambiar de usuario (D-078). */
export const CLAVE_DUENO_AVISOS = 'alimentarse.avisos';

/**
 * - `no-disponibles`: sin service worker (solo existe en el build de producción), sin API de notificaciones o sin
 *   claves VAPID en el servidor.
 * - `bloqueados`: el usuario ha denegado las notificaciones en el navegador.
 */
export type EstadoAvisos =
  'comprobando' | 'no-disponibles' | 'bloqueados' | 'activados' | 'desactivados';

/**
 * Avisos push del recordatorio de las comidas planificadas, en este navegador (D-078). La suscripción la gestiona el
 * service worker de Angular, que también muestra los avisos.
 */
@Injectable({ providedIn: 'root' })
export class AvisosPush {
  private readonly http = inject(HttpClient);
  private readonly sw = inject(SwPush, { optional: true });
  private readonly suscripcion = toSignal(
    this.sw?.isEnabled ? this.sw.subscription : (NEVER as Observable<PushSubscription | null>),
    { initialValue: null },
  );
  private readonly permiso = signal(permisoActual());
  /** `undefined` mientras no se ha preguntado al servidor; `null` si no tiene avisos. */
  private readonly clave = signal<string | null | undefined>(undefined);

  readonly estado = computed<EstadoAvisos>(() => {
    if (!this.compatible()) {
      return 'no-disponibles';
    }
    const clave = this.clave();
    if (clave === undefined) {
      return 'comprobando';
    }
    if (clave === null) {
      return 'no-disponibles';
    }
    if (this.permiso() === 'denied') {
      return 'bloqueados';
    }
    return this.suscripcion() ? 'activados' : 'desactivados';
  });

  /** Pregunta al servidor si tiene avisos (su clave pública VAPID). */
  async comprobar(): Promise<void> {
    if (!this.compatible() || this.clave() !== undefined) {
      return;
    }
    try {
      const respuesta = await firstValueFrom(
        this.http.get<Esquemas['ClavePushDto']>('/api/push/clave'),
      );
      this.clave.set(respuesta.clavePublica);
    } catch {
      this.clave.set(null);
    }
  }

  /** Pide permiso al navegador, se suscribe y guarda la suscripción en el servidor para este usuario. */
  async activar(usuario: string): Promise<void> {
    await this.comprobar();
    const clave = this.clave();
    if (!this.sw || !clave) {
      throw new Error('Los avisos no están disponibles');
    }
    try {
      const suscripcion = await this.sw.requestSubscription({ serverPublicKey: clave });
      const { endpoint, keys } = suscripcion.toJSON();
      try {
        await firstValueFrom(
          this.http.put<void>('/api/push/suscripcion', {
            endpoint: endpoint!,
            p256dh: keys!['p256dh'],
            auth: keys!['auth'],
            zona: Intl.DateTimeFormat().resolvedOptions().timeZone ?? null,
          } satisfies Esquemas['SuscripcionPushRequest']),
        );
      } catch (error) {
        // Si el servidor no la guarda (por ejemplo, con el tope de navegadores, D-089), este navegador no recibiría
        // nada: que tampoco parezca suscrito
        await this.sw.unsubscribe().catch(() => undefined);
        throw error;
      }
      guardarDueno(usuario);
    } finally {
      this.permiso.set(permisoActual());
    }
  }

  /** Deja de recibir avisos en este navegador. */
  async desactivar(): Promise<void> {
    const actual = this.suscripcion();
    if (!this.sw || !actual) {
      return;
    }
    try {
      await firstValueFrom(
        this.http.delete<void>('/api/push/suscripcion', { params: { endpoint: actual.endpoint } }),
      );
    } finally {
      // Aunque falle el servidor, el navegador deja de estar suscrito: el siguiente aviso le dará un 410 y la borrará
      await this.sw.unsubscribe();
      guardarDueno(null);
    }
  }

  probar(): Observable<PruebaPush> {
    return this.http.post<PruebaPush>('/api/push/prueba', null);
  }

  /** Al cerrar sesión: los avisos de este navegador dejan de ser de esta persona. Sin errores. */
  async alCerrarSesion(): Promise<void> {
    try {
      await this.desactivar();
    } catch {
      // Sin conexión o con la sesión ya caducada: el navegador ya ha dejado de estar suscrito
    }
  }

  /**
   * Al iniciar sesión: si este navegador recibía los avisos de otra persona (no cerró sesión), dejan de llegar. Su
   * suscripción en el servidor se borrará con el primer aviso que el servicio push rechace.
   */
  async alIniciarSesion(usuario: string): Promise<void> {
    if (!this.sw?.isEnabled) {
      return;
    }
    try {
      const actual = await firstValueFrom(this.sw.subscription.pipe(take(1)));
      if (actual && leerDueno() !== usuario) {
        await this.sw.unsubscribe();
        guardarDueno(null);
      }
    } catch {
      // El navegador no deja consultar la suscripción: nada que hacer
    }
  }

  private compatible(): boolean {
    return !!this.sw?.isEnabled && typeof Notification !== 'undefined';
  }
}

function permisoActual(): NotificationPermission {
  return typeof Notification === 'undefined' ? 'default' : Notification.permission;
}

function leerDueno(): string | null {
  try {
    return localStorage.getItem(CLAVE_DUENO_AVISOS);
  } catch {
    return null;
  }
}

function guardarDueno(usuario: string | null): void {
  try {
    if (usuario) {
      localStorage.setItem(CLAVE_DUENO_AVISOS, usuario);
    } else {
      localStorage.removeItem(CLAVE_DUENO_AVISOS);
    }
  } catch {
    // Sin almacenamiento local: se pierde el dueño y, al iniciar sesión, se anularía la suscripción
  }
}
