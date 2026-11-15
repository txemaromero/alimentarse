import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ErrorCampo } from '../compartido/error-campo';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { noVacio } from '../compartido/validadores';
import { AccesoGoogle } from '../core/acceso-google';
import { AuthApi } from '../core/auth-api';
import { AvisosPush } from '../core/avisos-push';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { destinoSeguro } from '../core/guards';
import { DatosRegistroGoogle } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { PATRON_ID_USUARIO } from './registro';

/**
 * Última parte del registro con Google (D-085): quien entra por primera vez elige su nombre de usuario y revisa el
 * nombre y los apellidos que da Google. El email es el de Google y no se cambia aquí.
 */
@Component({
  selector: 'app-registro-google',
  imports: [ReactiveFormsModule, RouterLink, ErrorCampo, RequiereConexion],
  templateUrl: './registro-google.html',
})
export class RegistroGoogle {
  private readonly authApi = inject(AuthApi);
  private readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(Notificaciones);
  private readonly avisos = inject(AvisosPush);
  private readonly accesoGoogle = inject(AccesoGoogle);

  readonly formulario = inject(NonNullableFormBuilder).group({
    id: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(30),
        Validators.pattern(PATRON_ID_USUARIO),
      ],
    ],
    nombre: ['', [Validators.required, noVacio, Validators.maxLength(100)]],
    apellido1: ['', [Validators.required, noVacio, Validators.maxLength(100)]],
    apellido2: ['', Validators.maxLength(100)],
  });
  readonly cargando = signal(true);
  /** No hay registro pendiente: nunca lo hubo o han pasado más de 15 minutos. */
  readonly caducado = signal(false);
  readonly fallo = signal(false);
  readonly datos = signal<DatosRegistroGoogle | null>(null);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  protected readonly esInvalido = esInvalido;

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.fallo.set(false);
    this.authApi.registroGoogle().subscribe({
      next: (datos) => {
        this.datos.set(datos);
        this.formulario.patchValue({
          nombre: datos.nombre,
          apellido1: datos.apellido1,
          apellido2: datos.apellido2 ?? '',
        });
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        if (error.status === 404) {
          this.caducado.set(true);
        } else {
          this.fallo.set(true);
        }
      },
    });
  }

  volverAGoogle(): void {
    this.accesoGoogle.entrar(this.accesoGoogle.destino());
  }

  registrar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const valores = this.formulario.getRawValue();
    this.authApi
      .completarRegistroGoogle({
        id: valores.id.trim(),
        nombre: valores.nombre.trim(),
        apellido1: valores.apellido1.trim(),
        apellido2: valores.apellido2.trim() || null,
      })
      .subscribe({
        next: (respuesta) => {
          this.sesion.iniciar(respuesta);
          void this.avisos.alIniciarSesion(respuesta.usuario.id);
          this.notificaciones.exito(`Cuenta creada. ¡Hola, ${respuesta.usuario.nombre}!`);
          this.router.navigateByUrl(destinoSeguro(this.accesoGoogle.destino()), {
            replaceUrl: true,
          });
        },
        error: (error: HttpErrorResponse) => {
          this.enviando.set(false);
          if (error.status === 404) {
            this.caducado.set(true);
          } else if (error.status >= 400 && error.status < 500) {
            // El nombre de usuario ya usado va junto a su campo
            if (!aplicarErroresServidor(this.formulario, errorApi(error))) {
              this.error.set(mensajeError(error));
            }
          }
        },
      });
  }
}
