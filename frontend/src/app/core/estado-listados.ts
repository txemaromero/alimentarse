import { Injectable } from '@angular/core';

/**
 * Recuerda, mientras dura la pestaña, el filtro y la página de cada listado. Así, al volver del detalle de una receta
 * el listado aparece como se dejó. Se vacía al cerrar la sesión.
 */
@Injectable({ providedIn: 'root' })
export class EstadoListados {
  private readonly estados = new Map<string, { filtro: unknown; pagina: number }>();

  leer<F>(clave: string): { filtro: F; pagina: number } | undefined {
    return this.estados.get(clave) as { filtro: F; pagina: number } | undefined;
  }

  guardar<F>(clave: string, filtro: F, pagina: number): void {
    this.estados.set(clave, { filtro, pagina });
  }

  vaciar(): void {
    this.estados.clear();
  }
}
