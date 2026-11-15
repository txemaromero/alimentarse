import { inject } from '@angular/core';
import { CanActivateFn, CanDeactivateFn, Router } from '@angular/router';
import { Notificaciones } from './notificaciones';
import { Sesion } from './sesion';

/** Exige sesión; si no la hay, lleva al login recordando la página pedida. */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const sesion = inject(Sesion);
  if (sesion.vigente()) {
    return true;
  }
  const queryParams = estado.url === '/' ? {} : { returnUrl: estado.url };
  return inject(Router).createUrlTree(['/login'], { queryParams });
};

/** Exige el rol ADMIN (después de authGuard). */
export const adminGuard: CanActivateFn = () => {
  if (inject(Sesion).esAdmin()) {
    return true;
  }
  inject(Notificaciones).aviso('Esa sección es solo para administradores.');
  return inject(Router).createUrlTree(['/']);
};

/** Login, registro y recuperación: si ya hay sesión, a la portada. */
export const invitadoGuard: CanActivateFn = () => {
  const sesion = inject(Sesion);
  return sesion.vigente() ? inject(Router).createUrlTree(['/']) : true;
};

/**
 * Destino tras el login: solo rutas internas de la aplicación, para no poder redirigir a otro sitio con un enlace
 * manipulado (`?returnUrl=//malicioso.com`).
 */
export function destinoSeguro(url: string | null | undefined): string {
  if (
    !url ||
    !url.startsWith('/') ||
    url.startsWith('//') ||
    url.startsWith('/\\') ||
    url.startsWith('/login')
  ) {
    return '/';
  }
  return url;
}

/** Componente con un formulario que puede tener cambios sin guardar. */
export interface ConCambiosPendientes {
  hayCambiosSinGuardar(): boolean;
}

/** Pide confirmación antes de salir de un formulario con cambios sin guardar. */
export const sinCambiosPendientes: CanDeactivateFn<ConCambiosPendientes> = (componente) =>
  !componente.hayCambiosSinGuardar() ||
  window.confirm('Hay cambios sin guardar. ¿Quieres salir sin guardarlos?');
