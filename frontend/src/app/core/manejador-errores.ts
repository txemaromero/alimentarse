import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandler, Injectable, inject } from '@angular/core';
import { MENSAJE_ERROR_GENERICO } from './errores';
import { Notificaciones } from './notificaciones';

/**
 * Errores no capturados de la aplicación: se registran en la consola y se avisa al usuario. Los errores HTTP no se
 * avisan aquí porque ya lo hace el interceptor o la pantalla correspondiente.
 */
@Injectable()
export class ManejadorErroresGlobal implements ErrorHandler {
  private readonly notificaciones = inject(Notificaciones);

  handleError(error: unknown): void {
    console.error(error);
    if (!(error instanceof HttpErrorResponse)) {
      this.notificaciones.error(MENSAJE_ERROR_GENERICO);
    }
  }
}
