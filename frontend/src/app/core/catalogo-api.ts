import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { Grupo, TipoReceta } from './modelos';

/**
 * Catálogos (grupos de alimentos y tipos de receta). Cambian muy poco, así que se piden una vez y se guardan. Los
 * grupos se pueden editar desde la administración (D-065): después se llama a `invalidarGrupos()` y la siguiente
 * pantalla que los pida los vuelve a descargar.
 */
@Injectable({ providedIn: 'root' })
export class CatalogoApi {
  private readonly http = inject(HttpClient);
  private grupos?: Observable<Grupo[]>;

  get grupos$(): Observable<Grupo[]> {
    this.grupos ??= this.http
      .get<Grupo[]>('/api/grupos')
      .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    return this.grupos;
  }

  readonly tipos$: Observable<TipoReceta[]> = this.http
    .get<TipoReceta[]>('/api/tipos-receta')
    .pipe(shareReplay({ bufferSize: 1, refCount: false }));

  /** Tras crear, editar o borrar un grupo en la administración. */
  invalidarGrupos(): void {
    this.grupos = undefined;
  }
}

/** «(N raciones/semana)» o «(sin límite)», como en las tarjetas de ingrediente (6.8). */
export function textoRaciones(grupo: Pick<Grupo, 'racionesSemana'>): string {
  if (grupo.racionesSemana === null) {
    return 'sin límite';
  }
  return grupo.racionesSemana === 1 ? '1 ración/semana' : `${grupo.racionesSemana} raciones/semana`;
}
