import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ComidaPlanificada, Consumo, type Esquemas, ListaCompra, Plan } from '../core/modelos';

/** Planificador semanal del usuario actual (D-068). */
@Injectable({ providedIn: 'root' })
export class PlanApi {
  private readonly http = inject(HttpClient);

  /** Comidas del intervalo y cómo quedarían las raciones al final de él. */
  obtener(desde: Date, hasta: Date): Observable<Plan> {
    const params = new HttpParams()
      .set('desde', desde.toISOString())
      .set('hasta', hasta.toISOString());
    return this.http.get<Plan>('/api/plan', { params });
  }

  /** Lista de la compra de las comidas del intervalo que aún no han pasado (D-076). */
  compra(desde: Date, hasta: Date): Observable<ListaCompra> {
    const params = new HttpParams()
      .set('desde', desde.toISOString())
      .set('hasta', hasta.toISOString());
    return this.http.get<ListaCompra>('/api/plan/compra', { params });
  }

  anadir(receta: number, fecha: Date): Observable<ComidaPlanificada> {
    return this.http.post<ComidaPlanificada>('/api/plan', {
      receta,
      fecha: fecha.toISOString(),
    } satisfies Esquemas['ComidaPlanRequest']);
  }

  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`/api/plan/${id}`);
  }

  /** La comida pasa a los consumos y sale del plan. */
  consumir(id: number): Observable<Consumo> {
    return this.http.post<Consumo>(`/api/plan/${id}/consumir`, null);
  }
}
