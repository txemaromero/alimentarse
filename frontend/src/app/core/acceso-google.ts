import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/** Donde empieza el acceso con Google en el backend (Spring Security, D-085). */
export const URL_ACCESO_GOOGLE = '/oauth2/authorization/google';

const CLAVE_DESTINO = 'alimentarse.destino-google';

/**
 * Entrar con Google (D-085). Es una navegación completa, fuera de Angular: el backend lleva a Google y, a la vuelta, a
 * `/login/google` o a `/registro/google`. La página a la que volver después (`returnUrl`) se guarda mientras tanto en
 * `sessionStorage`.
 */
@Injectable({ providedIn: 'root' })
export class AccesoGoogle {
  private readonly documento = inject(DOCUMENT);

  entrar(destino?: string): void {
    try {
      if (destino) {
        sessionStorage.setItem(CLAVE_DESTINO, destino);
      } else {
        sessionStorage.removeItem(CLAVE_DESTINO);
      }
    } catch {
      // Sin sessionStorage se vuelve a la portada
    }
    this.documento.location.assign(URL_ACCESO_GOOGLE);
  }

  /** La página a la que volver tras entrar, que se olvida al leerla. */
  destino(): string | undefined {
    try {
      const destino = sessionStorage.getItem(CLAVE_DESTINO) ?? undefined;
      sessionStorage.removeItem(CLAVE_DESTINO);
      return destino;
    } catch {
      return undefined;
    }
  }
}
