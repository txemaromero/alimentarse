import { DOCUMENT, DestroyRef, Injectable, inject, signal } from '@angular/core';

/** Estado de la conexión del navegador (`navigator.onLine` y los eventos `online`/`offline`). */
@Injectable({ providedIn: 'root' })
export class Conexion {
  private readonly ventana = inject(DOCUMENT).defaultView;
  private readonly estado = signal(this.ventana?.navigator.onLine ?? true);
  readonly enLinea = this.estado.asReadonly();

  constructor() {
    const ventana = this.ventana;
    if (!ventana) {
      return;
    }
    const actualizar = () => this.estado.set(ventana.navigator.onLine);
    ventana.addEventListener('online', actualizar);
    ventana.addEventListener('offline', actualizar);
    inject(DestroyRef).onDestroy(() => {
      ventana.removeEventListener('online', actualizar);
      ventana.removeEventListener('offline', actualizar);
    });
  }
}
