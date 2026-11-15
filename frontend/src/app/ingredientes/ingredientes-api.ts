import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Ingrediente, IngredienteRequest, Pagina } from '../core/modelos';

export const INGREDIENTES_POR_PAGINA = 16;

export interface FiltroIngredientes {
  nombre?: string;
  grupo?: number;
  /** Solo los que no tienen alimento de BEDCA (administración, D-080). */
  sinAlimento?: boolean;
}

@Injectable({ providedIn: 'root' })
export class IngredientesApi {
  private readonly http = inject(HttpClient);

  listar(
    filtro: FiltroIngredientes,
    pagina: number,
    tamano = INGREDIENTES_POR_PAGINA,
  ): Observable<Pagina<Ingrediente>> {
    let params = new HttpParams().set('page', pagina).set('size', tamano);
    if (filtro.nombre?.trim()) {
      params = params.set('nombre', filtro.nombre.trim());
    }
    if (filtro.grupo) {
      params = params.set('grupo', filtro.grupo);
    }
    if (filtro.sinAlimento) {
      params = params.set('sinAlimento', true);
    }
    return this.http.get<Pagina<Ingrediente>>('/api/ingredientes', { params });
  }

  /** Autocompletado: hasta 10, primero los que empiezan por `q` (D-028). */
  sugerencias(q: string): Observable<Ingrediente[]> {
    return this.http.get<Ingrediente[]>('/api/ingredientes/sugerencias', { params: { q } });
  }

  crear(datos: IngredienteRequest): Observable<Ingrediente> {
    return this.http.post<Ingrediente>('/api/ingredientes', datos);
  }

  /** Solo ADMIN. */
  editar(id: number, datos: IngredienteRequest): Observable<Ingrediente> {
    return this.http.put<Ingrediente>(`/api/ingredientes/${id}`, datos);
  }
}
