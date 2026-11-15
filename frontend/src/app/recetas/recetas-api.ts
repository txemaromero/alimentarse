import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  type Esquemas,
  FiltroRecetas,
  ImpactoGrupo,
  NutricionReceta,
  Likes,
  Pagina,
  RecetaDetalle,
  RecetaRecomendada,
  RecetaRequest,
  RecetaResumen,
  ResumenValoraciones,
  SugerenciaReceta,
  Valoracion,
} from '../core/modelos';

export const RECETAS_POR_PAGINA = 9;

/** Parámetros de consulta de los filtros 6.5 (solo los que tienen valor). */
export function parametrosFiltro(
  filtro: FiltroRecetas,
  pagina: number,
  tamano: number,
): HttpParams {
  let params = new HttpParams().set('page', pagina).set('size', tamano);
  const texto = (clave: 'titulo' | 'usuario') => filtro[clave]?.trim();
  if (texto('titulo')) {
    params = params.set('titulo', texto('titulo')!);
  }
  if (texto('usuario')) {
    params = params.set('usuario', texto('usuario')!);
  }
  if (filtro.tipo) {
    params = params.set('tipo', filtro.tipo);
  }
  if (filtro.dificultad) {
    params = params.set('dificultad', filtro.dificultad);
  }
  if (filtro.comensales) {
    params = params.set('comensales', filtro.comensales);
  }
  if (filtro.tiempoMax) {
    params = params.set('tiempoMax', filtro.tiempoMax);
  }
  if (filtro.ingredientes?.length) {
    params = params.set('ingredientes', filtro.ingredientes.map((i) => i.id).join(','));
  }
  return params;
}

@Injectable({ providedIn: 'root' })
export class RecetasApi {
  private readonly http = inject(HttpClient);

  /** Recetas publicadas con los filtros 6.5. */
  buscar(
    filtro: FiltroRecetas,
    pagina: number,
    tamano = RECETAS_POR_PAGINA,
  ): Observable<Pagina<RecetaResumen>> {
    return this.http.get<Pagina<RecetaResumen>>('/api/recetas', {
      params: parametrosFiltro(filtro, pagina, tamano),
    });
  }

  /** Recetas propias, publicadas o no. */
  mias(
    filtro: FiltroRecetas,
    pagina: number,
    tamano = RECETAS_POR_PAGINA,
  ): Observable<Pagina<RecetaResumen>> {
    return this.http.get<Pagina<RecetaResumen>>('/api/recetas/mias', {
      params: parametrosFiltro(filtro, pagina, tamano),
    });
  }

  /** Publicadas a las que el usuario ha dado «me gusta», de la más reciente a la más antigua (D-062). */
  meGustan(
    filtro: FiltroRecetas,
    pagina: number,
    tamano = RECETAS_POR_PAGINA,
  ): Observable<Pagina<RecetaResumen>> {
    return this.http.get<Pagina<RecetaResumen>>('/api/recetas/me-gustan', {
      params: parametrosFiltro(filtro, pagina, tamano),
    });
  }

  /**
   * «¿Qué toca hoy?» (6.3): publicadas sin ingredientes de los grupos que el usuario ya ha agotado en los últimos 7
   * días, con los mismos filtros. Primero las de los grupos menos consumidos esta semana (D-067).
   */
  recomendadas(
    filtro: FiltroRecetas,
    pagina: number,
    tamano = RECETAS_POR_PAGINA,
  ): Observable<Pagina<RecetaRecomendada>> {
    return this.http.get<Pagina<RecetaRecomendada>>('/api/recetas/recomendadas', {
      params: parametrosFiltro(filtro, pagina, tamano),
    });
  }

  /** Cómo quedarían los grupos con límite de la receta si el usuario la comiera ahora (D-067). */
  impacto(id: number): Observable<ImpactoGrupo[]> {
    return this.http.get<ImpactoGrupo[]>(`/api/recetas/${id}/impacto`);
  }

  /** Estimación nutricional de la receta, por ración y en total (D-079). */
  nutricion(id: number): Observable<NutricionReceta> {
    return this.http.get<NutricionReceta>(`/api/recetas/${id}/nutricion`);
  }

  mejorValoradas(limite = 3): Observable<RecetaResumen[]> {
    return this.http.get<RecetaResumen[]>('/api/recetas/mejor-valoradas', {
      params: { limit: limite },
    });
  }

  ultimas(limite = 3): Observable<RecetaResumen[]> {
    return this.http.get<RecetaResumen[]>('/api/recetas/ultimas', { params: { limit: limite } });
  }

  sugerencias(q: string): Observable<SugerenciaReceta[]> {
    return this.http.get<SugerenciaReceta[]>('/api/recetas/sugerencias', { params: { q } });
  }

  detalle(id: number): Observable<RecetaDetalle> {
    return this.http.get<RecetaDetalle>(`/api/recetas/${id}`);
  }

  crear(receta: RecetaRequest, fotos: File[] = []): Observable<RecetaDetalle> {
    return this.http.post<RecetaDetalle>('/api/recetas', cuerpoMultipart(receta, fotos));
  }

  editar(id: number, receta: RecetaRequest, fotos: File[] = []): Observable<RecetaDetalle> {
    return this.http.put<RecetaDetalle>(`/api/recetas/${id}`, cuerpoMultipart(receta, fotos));
  }

  publicar(id: number, publicada: boolean): Observable<RecetaDetalle> {
    return this.http.patch<RecetaDetalle>(`/api/recetas/${id}/publicada`, {
      publicada,
    } satisfies Esquemas['PublicacionRequest']);
  }

  /**
   * Solo el autor. Si alguien ya la había consumido, sigue en su histórico como «eliminada» y contando en sus raciones
   * (D-061).
   */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`/api/recetas/${id}`);
  }

  /** Da o quita «me gusta» según `meGusta`. */
  meGusta(id: number, meGusta: boolean): Observable<Likes> {
    const url = `/api/recetas/${id}/like`;
    return meGusta ? this.http.post<Likes>(url, null) : this.http.delete<Likes>(url);
  }

  /** Valoraciones de la receta, de la más reciente a la más antigua (D-077). */
  valoraciones(
    id: number,
    pagina: number,
    tamano = VALORACIONES_POR_PAGINA,
  ): Observable<Pagina<Valoracion>> {
    return this.http.get<Pagina<Valoracion>>(`/api/recetas/${id}/valoraciones`, {
      params: { page: pagina, size: tamano },
    });
  }

  /** Crea o cambia la valoración propia. */
  valorar(id: number, nota: number, comentario: string | null): Observable<ResumenValoraciones> {
    return this.http.put<ResumenValoraciones>(`/api/recetas/${id}/valoracion`, {
      nota,
      comentario,
    } satisfies Esquemas['ValoracionRequest']);
  }

  borrarValoracion(id: number): Observable<ResumenValoraciones> {
    return this.http.delete<ResumenValoraciones>(`/api/recetas/${id}/valoracion`);
  }
}

/** Valoraciones que se cargan cada vez («Ver más»). */
export const VALORACIONES_POR_PAGINA = 5;

/**
 * Cuerpo multipart (D-036, D-083): la parte `receta` como JSON y una parte `fotos` por cada fichero nuevo, en el orden
 * al que apuntan las `nueva` de `receta.fotos`. No se fija el Content-Type: lo pone el navegador, con el separador.
 */
export function cuerpoMultipart(receta: RecetaRequest, fotos: File[]): FormData {
  const datos = new FormData();
  datos.append('receta', new Blob([JSON.stringify(receta)], { type: 'application/json' }));
  for (const foto of fotos) {
    datos.append('fotos', foto);
  }
  return datos;
}
