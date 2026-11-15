import { Injectable, signal } from '@angular/core';

export type TipoAviso = 'exito' | 'error' | 'info' | 'aviso';

export interface Aviso {
  id: number;
  tipo: TipoAviso;
  texto: string;
}

/** Avisos emergentes (toasts) de la aplicación. Los muestra el componente Avisos. */
@Injectable({ providedIn: 'root' })
export class Notificaciones {
  private siguienteId = 1;
  readonly avisos = signal<Aviso[]>([]);

  exito(texto: string): void {
    this.mostrar('exito', texto);
  }

  error(texto: string): void {
    this.mostrar('error', texto);
  }

  info(texto: string): void {
    this.mostrar('info', texto);
  }

  aviso(texto: string): void {
    this.mostrar('aviso', texto);
  }

  quitar(id: number): void {
    this.avisos.update((avisos) => avisos.filter((aviso) => aviso.id !== id));
  }

  private mostrar(tipo: TipoAviso, texto: string): void {
    // No se repite un aviso idéntico que ya está en pantalla
    if (this.avisos().some((aviso) => aviso.tipo === tipo && aviso.texto === texto)) {
      return;
    }
    this.avisos.update((avisos) => [...avisos, { id: this.siguienteId++, tipo, texto }]);
  }
}
