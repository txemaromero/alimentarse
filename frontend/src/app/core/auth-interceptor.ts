import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { MENSAJE_ERROR_GENERICO, errorApi, mensajeError } from './errores';
import { Notificaciones } from './notificaciones';
import { Sesion } from './sesion';

/**
 * La sesión viaja en una cookie que pone el navegador (D-070). Ante un 401 cierra la sesión y lleva al login conservando la página actual
 * (`returnUrl`); si es porque la administración ha bloqueado la cuenta, lo dice (D-063). Los fallos de red y los errores 5xx se avisan aquí; el resto lo trata cada pantalla.
 */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  const notificaciones = inject(Notificaciones);

  const esApi = peticion.url.startsWith('/api/');
  const esAutenticacion = peticion.url.startsWith('/api/auth/');

  return siguiente(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && esApi && !esAutenticacion) {
        sesion.cerrar();
        notificaciones.aviso(
          errorApi(error)?.codigo === 'CUENTA_BLOQUEADA'
            ? mensajeError(error)
            : 'Tu sesión ha caducado o no es válida. Inicia sesión de nuevo.',
        );
        const actual = router.url;
        router.navigate(['/login'], {
          queryParams: actual.startsWith('/login') ? {} : { returnUrl: actual },
        });
      } else if (error.status === 0) {
        notificaciones.error('No se puede conectar con el servidor. Comprueba tu conexión.');
      } else if (error.status >= 500) {
        notificaciones.error(mensajeError(error, MENSAJE_ERROR_GENERICO));
      }
      return throwError(() => error);
    }),
  );
};
