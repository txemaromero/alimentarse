import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Consumo,
  type Esquemas,
  NutricionSemana,
  Pagina,
  ResumenAlimentacion,
} from '../core/modelos';

export const CONSUMOS_POR_PAGINA = 9;

/** Consumos del usuario actual (6.7) y resumen de «Mi alimentación». */
@Injectable({ providedIn: 'root' })
export class ConsumosApi {
  private readonly http = inject(HttpClient);

  /** «Añadir a consumidos» (6.7). `fecha` en ISO-8601 UTC; sin ella, el servidor usa «ahora». */
  registrar(receta: number, fecha?: string): Observable<Consumo> {
    return this.http.post<Consumo>('/api/consumos', {
      receta,
      fecha: fecha ?? null,
    } satisfies Esquemas['ConsumoRequest']);
  }

  /** Histórico, del más reciente al más antiguo. */
  historico(pagina: number, tamano = CONSUMOS_POR_PAGINA): Observable<Pagina<Consumo>> {
    const params = new HttpParams().set('page', pagina).set('size', tamano);
    return this.http.get<Pagina<Consumo>>('/api/consumos', { params });
  }

  cambiarFecha(id: number, fecha: string): Observable<Consumo> {
    return this.http.put<Consumo>(`/api/consumos/${id}`, {
      fecha,
    } satisfies Esquemas['FechaConsumoRequest']);
  }

  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`/api/consumos/${id}`);
  }

  resumen(): Observable<ResumenAlimentacion> {
    return this.http.get<ResumenAlimentacion>('/api/alimentacion/resumen');
  }

  /** Nutrientes de lo comido en los últimos 7 días: una ración por consumo (D-082). */
  nutricionSemana(): Observable<NutricionSemana> {
    return this.http.get<NutricionSemana>('/api/alimentacion/nutricion');
  }
}
