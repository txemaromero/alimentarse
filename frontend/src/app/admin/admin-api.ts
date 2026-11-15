import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AlimentoBedca,
  type Esquemas,
  FusionIngredientes,
  GrupoAdmin,
  GrupoRequest,
  Ingrediente,
  NutricionIngrediente,
  Pagina,
  RecetaDetalle,
  RecetaResumen,
  UsuarioAdmin,
  Valoracion,
} from '../core/modelos';
import { RECETAS_POR_PAGINA } from '../recetas/recetas-api';

export const USUARIOS_POR_PAGINA = 10;

/** Gestión de usuarios, moderación y catálogo (solo ADMIN, D-043, D-063 y D-065). */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  /** `q` busca en el id, el nombre, los apellidos o el email (D-044). */
  buscar(
    q: string | undefined,
    pagina: number,
    tamano = USUARIOS_POR_PAGINA,
  ): Observable<Pagina<UsuarioAdmin>> {
    let params = new HttpParams().set('page', pagina).set('size', tamano);
    if (q?.trim()) {
      params = params.set('q', q.trim());
    }
    return this.http.get<Pagina<UsuarioAdmin>>('/api/admin/usuarios', { params });
  }

  /** Da o quita el rol ADMIN. Esa persona tiene que volver a iniciar sesión (D-070). */
  cambiarAdmin(id: string, admin: boolean): Observable<UsuarioAdmin> {
    return this.http.put<UsuarioAdmin>(`/api/admin/usuarios/${encodeURIComponent(id)}/admin`, {
      admin,
    } satisfies Esquemas['CambioAdminRequest']);
  }

  /** Un usuario bloqueado no puede iniciar sesión y pierde la sesión abierta al momento (D-063). */
  cambiarBloqueo(id: string, bloqueado: boolean): Observable<UsuarioAdmin> {
    return this.http.put<UsuarioAdmin>(`/api/admin/usuarios/${encodeURIComponent(id)}/bloqueado`, {
      bloqueado,
    } satisfies Esquemas['CambioBloqueoRequest']);
  }

  /** Oculta una receta con un motivo, que verá su autor, o la vuelve a mostrar. */
  ocultarReceta(id: number, oculta: boolean, motivo?: string): Observable<RecetaDetalle> {
    return this.http.put<RecetaDetalle>(`/api/admin/recetas/${id}/oculta`, {
      oculta,
      motivo: motivo ?? null,
    } satisfies Esquemas['ModeracionRecetaRequest']);
  }

  /** Oculta una valoración, que deja de verse y de contar en la media, o la vuelve a mostrar (D-077). */
  ocultarValoracion(id: number, oculta: boolean): Observable<Valoracion> {
    return this.http.put<Valoracion>(`/api/admin/valoraciones/${id}/oculta`, {
      oculta,
    } satisfies Esquemas['OcultarValoracionRequest']);
  }

  recetasOcultas(pagina: number, tamano = RECETAS_POR_PAGINA): Observable<Pagina<RecetaResumen>> {
    return this.http.get<Pagina<RecetaResumen>>('/api/admin/recetas/ocultas', {
      params: { page: pagina, size: tamano },
    });
  }

  /** Grupos de alimentos con sus raciones, si son básicos y cuántos ingredientes tienen. */
  grupos(): Observable<GrupoAdmin[]> {
    return this.http.get<GrupoAdmin[]>('/api/admin/grupos');
  }

  crearGrupo(datos: GrupoRequest): Observable<GrupoAdmin> {
    return this.http.post<GrupoAdmin>('/api/admin/grupos', datos);
  }

  editarGrupo(id: number, datos: GrupoRequest): Observable<GrupoAdmin> {
    return this.http.put<GrupoAdmin>(`/api/admin/grupos/${id}`, datos);
  }

  /** 409 `GRUPO_EN_USO` si tiene ingredientes. */
  borrarGrupo(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/grupos/${id}`);
  }

  /** Las recetas de `origen` pasan a `destino` y `origen` se borra. */
  fusionarIngrediente(origen: number, destino: number): Observable<FusionIngredientes> {
    return this.http.post<FusionIngredientes>(`/api/admin/ingredientes/${origen}/fusionar`, {
      destino,
    } satisfies Esquemas['FusionRequest']);
  }

  /** 409 `INGREDIENTE_EN_USO` si lo usa alguna receta. */
  borrarIngrediente(id: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/ingredientes/${id}`);
  }

  /** Alimentos de BEDCA cuyo nombre contiene todas las palabras de `q`, hasta 10 (D-080). */
  alimentosBedca(q: string): Observable<AlimentoBedca[]> {
    return this.http.get<AlimentoBedca[]>('/api/admin/alimentos-bedca', { params: { q } });
  }

  /** El alimento de BEDCA y los pesos de las piezas del ingrediente (D-080, D-081). */
  nutricionIngrediente(ingrediente: number): Observable<NutricionIngrediente> {
    return this.http.get<NutricionIngrediente>(`/api/admin/ingredientes/${ingrediente}/nutricion`);
  }

  /** Sustituye el alimento de BEDCA (`null`, ninguno) y las piezas del ingrediente (D-080, D-081). */
  guardarNutricion(
    ingrediente: number,
    datos: Esquemas['NutricionIngredienteRequest'],
  ): Observable<Ingrediente> {
    return this.http.put<Ingrediente>(`/api/admin/ingredientes/${ingrediente}/nutricion`, datos);
  }
}
