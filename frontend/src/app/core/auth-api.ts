import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DatosRegistroGoogle,
  DatosVinculacionGoogle,
  EmailConfirmado,
  type Esquemas,
  LoginResponse,
  MensajeRespuesta,
  Perfil,
  Proveedores,
  Registro,
  RegistroGoogle,
} from './modelos';

/** Endpoints públicos de autenticación (/api/auth). */
@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);

  login(usuario: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', {
      usuario,
      password,
    } satisfies Esquemas['LoginRequest']);
  }

  /** Las formas de entrar que hay además del usuario y la contraseña (D-085). */
  proveedores(): Observable<Proveedores> {
    return this.http.get<Proveedores>('/api/auth/proveedores');
  }

  /** Los datos de la sesión de la cookie, al volver de entrar con Google (D-085). 401 si no hay sesión. */
  sesion(): Observable<LoginResponse> {
    return this.http.get<LoginResponse>('/api/auth/sesion');
  }

  /** El email de la cuenta que se va a vincular con Google (D-086); 404 si no hay vinculación pendiente. */
  vinculacionGoogle(): Observable<DatosVinculacionGoogle> {
    return this.http.get<DatosVinculacionGoogle>('/api/auth/google/vincular');
  }

  /** Vincula la cuenta con Google con su contraseña de Toca y entra. */
  vincularGoogle(password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/google/vincular', {
      password,
    } satisfies Esquemas['VincularGoogleRequest']);
  }

  /** Lo que da Google de quien termina de registrarse con su cuenta; 404 si no hay registro pendiente. */
  registroGoogle(): Observable<DatosRegistroGoogle> {
    return this.http.get<DatosRegistroGoogle>('/api/auth/google/registro');
  }

  /** Crea la cuenta vinculada con Google y entra con ella. */
  completarRegistroGoogle(datos: RegistroGoogle): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/google/registro', {
      id: datos.id,
      nombre: datos.nombre,
      apellido1: datos.apellido1,
      apellido2: datos.apellido2,
    } satisfies Esquemas['RegistroGoogleRequest']);
  }

  /** Borra la cookie de sesión de este navegador. */
  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null);
  }

  registro(datos: Registro): Observable<Perfil> {
    return this.http.post<Perfil>('/api/auth/registro', datos);
  }

  olvidoPassword(email: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>('/api/auth/password/olvido', {
      email,
    } satisfies Esquemas['OlvidoPasswordRequest']);
  }

  /** Confirma el email con el token del enlace del correo (D-088); no hace falta sesión. */
  confirmarEmail(token: string): Observable<EmailConfirmado> {
    return this.http.post<EmailConfirmado>('/api/auth/email/confirmar', {
      token,
    } satisfies Esquemas['ConfirmarEmailRequest']);
  }

  restablecerPassword(token: string, password: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>('/api/auth/password/restablecer', {
      token,
      password,
    } satisfies Esquemas['RestablecerPasswordRequest']);
  }
}
