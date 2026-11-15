import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ActualizarPerfil, EnlaceEnviado, type Esquemas, Perfil } from '../core/modelos';

/** Perfil del usuario actual (`/api/usuarios/me`). */
@Injectable({ providedIn: 'root' })
export class PerfilApi {
  private readonly http = inject(HttpClient);

  obtener(): Observable<Perfil> {
    return this.http.get<Perfil>('/api/usuarios/me');
  }

  actualizar(datos: ActualizarPerfil): Observable<Perfil> {
    return this.http.put<Perfil>('/api/usuarios/me', datos);
  }

  /**
   * Todos los datos del usuario en JSON (D-064). Sin pasar por el service worker (`ngsw-bypass`): son datos personales
   * y no deben quedarse en su caché.
   */
  descargarDatos(): Observable<Blob> {
    return this.http.get('/api/usuarios/me/datos', {
      responseType: 'blob',
      headers: { 'ngsw-bypass': 'true' },
    });
  }

  /** 204 si va bien; 400 `PASSWORD_INCORRECTA` (`errores.password`); 409 si es el último administrador. */
  darseDeBaja(password: string): Observable<void> {
    return this.http.post<void>('/api/usuarios/me/baja', {
      password,
    } satisfies Esquemas['BajaRequest']);
  }

  /** Vuelve a enviar el enlace para confirmar el email (D-088); 409 `EMAIL_YA_CONFIRMADO` si no hace falta. */
  reenviarConfirmacion(): Observable<EnlaceEnviado> {
    return this.http.post<EnlaceEnviado>('/api/usuarios/me/email/confirmacion', null);
  }

  /** Descarta el email nuevo que esperaba su confirmación (D-088). */
  cancelarCambioEmail(): Observable<Perfil> {
    return this.http.delete<Perfil>('/api/usuarios/me/email/pendiente');
  }

  /** Deja de entrar con Google (D-086); 400 `PASSWORD_INCORRECTA` (`errores.password`). */
  desvincularGoogle(password: string): Observable<Perfil> {
    return this.http.post<Perfil>('/api/usuarios/me/desvincular-google', {
      password,
    } satisfies Esquemas['DesvincularGoogleRequest']);
  }

  /** 204 si va bien; 400 `PASSWORD_INCORRECTA` (`errores.actual`) si la actual no coincide. */
  cambiarPassword(actual: string, nueva: string): Observable<void> {
    return this.http.put<void>('/api/usuarios/me/password', {
      actual,
      nueva,
    } satisfies Esquemas['CambioPasswordRequest']);
  }
}
