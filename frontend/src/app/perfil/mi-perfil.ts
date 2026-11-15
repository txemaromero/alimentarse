import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faDownload, faKey, faUserPen, faUserXmark } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Router } from '@angular/router';
import { ErrorCampo } from '../compartido/error-campo';
import {
  cabeEnBcrypt,
  coincideCon,
  noVacio,
  revalidarConfirmacion,
} from '../compartido/validadores';
import { AuthApi } from '../core/auth-api';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { Perfil } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { PerfilApi } from './perfil-api';
import { AvisosPerfil } from './avisos-perfil';
import { abrirModalBaja } from './modal-baja';
import { abrirModalDesvincularGoogle } from './modal-desvincular-google';
import { RequiereConexion } from '../compartido/requiere-conexion';

/**
 * «Mi perfil»: editar los datos personales, cambiar la contraseña, los avisos (D-078), descargar los datos y darse de
 * baja (D-064).
 */
@Component({
  selector: 'app-mi-perfil',
  imports: [ReactiveFormsModule, FaIconComponent, ErrorCampo, RequiereConexion, AvisosPerfil],
  templateUrl: './mi-perfil.html',
})
export class MiPerfil implements OnInit {
  private readonly api = inject(PerfilApi);
  private readonly sesion = inject(Sesion);
  private readonly notificaciones = inject(Notificaciones);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly modales = inject(NgbModal);
  private readonly router = inject(Router);
  private readonly authApi = inject(AuthApi);

  readonly perfil = signal<Perfil | null>(null);
  readonly cargando = signal(true);
  readonly fallo = signal(false);

  readonly datos = this.fb.group({
    nombre: ['', [Validators.required, noVacio, Validators.maxLength(100)]],
    apellido1: ['', [Validators.required, noVacio, Validators.maxLength(100)]],
    apellido2: ['', Validators.maxLength(100)],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    /** La contraseña actual: solo se pide si cambia el email (D-086). */
    password: [''],
  });
  /** Si el email del formulario no es el de la cuenta: entonces hace falta la contraseña. */
  readonly cambiaEmail = signal(false);
  readonly guardandoDatos = signal(false);
  readonly errorDatos = signal<string | null>(null);

  readonly password = this.fb.group({
    actual: ['', Validators.required],
    nueva: [
      '',
      [Validators.required, Validators.minLength(8), Validators.maxLength(72), cabeEnBcrypt],
    ],
    confirmacion: ['', [Validators.required, coincideCon('nueva')]],
  });
  readonly cambiandoPassword = signal(false);
  readonly errorPassword = signal<string | null>(null);
  readonly descargando = signal(false);
  readonly enviandoEnlace = signal(false);
  readonly enviandoConfirmacion = signal(false);

  protected readonly esInvalido = esInvalido;
  protected readonly iconos = {
    datos: faUserPen,
    password: faKey,
    descargar: faDownload,
    baja: faUserXmark,
  };

  constructor() {
    const destruccion = inject(DestroyRef);
    revalidarConfirmacion(this.password, 'nueva', 'confirmacion', destruccion);
    this.datos.controls.email.valueChanges
      .pipe(takeUntilDestroyed(destruccion))
      .subscribe(() => this.comprobarEmail());
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.fallo.set(false);
    this.api.obtener().subscribe({
      next: (perfil) => {
        this.mostrar(perfil);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.fallo.set(true);
      },
    });
  }

  /** Para el guard de salida. */
  /** Descarga el JSON con todos sus datos como fichero `alimentarse-<id>.json`. */
  descargarDatos(): void {
    if (this.descargando()) {
      return;
    }
    this.descargando.set(true);
    this.api.descargarDatos().subscribe({
      next: (datos) => {
        this.descargando.set(false);
        const url = URL.createObjectURL(datos);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `alimentarse-${this.perfil()?.id ?? 'datos'}.json`;
        enlace.click();
        URL.revokeObjectURL(url);
      },
      error: (error: HttpErrorResponse) => {
        this.descargando.set(false);
        if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          this.notificaciones.error('No se han podido descargar tus datos.');
        }
      },
    });
  }

  /** Tras la baja ya no hay sesión: se cierra y se vuelve al inicio de sesión. */
  darseDeBaja(): void {
    abrirModalBaja(this.modales).subscribe(() => {
      // Sin cambios pendientes: que el guard no pregunte al salir
      this.datos.markAsPristine();
      this.password.markAsPristine();
      this.sesion.cerrar();
      this.notificaciones.exito(
        'Tu cuenta se ha eliminado. Te hemos enviado un correo de confirmación.',
      );
      this.router.navigateByUrl('/login');
    });
  }

  hayCambiosSinGuardar(): boolean {
    return this.datos.dirty || this.password.dirty;
  }

  guardarDatos(): void {
    if (this.datos.invalid || this.guardandoDatos()) {
      this.datos.markAllAsTouched();
      return;
    }
    this.guardandoDatos.set(true);
    this.errorDatos.set(null);
    const v = this.datos.getRawValue();
    this.api
      .actualizar({
        nombre: v.nombre.trim(),
        apellido1: v.apellido1.trim(),
        apellido2: v.apellido2.trim() || null,
        email: v.email.trim(),
        password: this.cambiaEmail() ? v.password : null,
      })
      .subscribe({
        next: (perfil) => {
          this.guardandoDatos.set(false);
          const cambiaEmail = this.cambiaEmail();
          this.mostrar(perfil);
          this.sesion.actualizarNombre(perfil.nombre);
          this.notificaciones.exito(
            cambiaEmail && perfil.emailPendiente
              ? `Datos guardados. Te hemos enviado un enlace a ${perfil.emailPendiente} para confirmar el cambio.`
              : 'Datos guardados.',
          );
        },
        error: (error: HttpErrorResponse) => {
          this.guardandoDatos.set(false);
          if (error.status >= 400 && error.status < 500 && error.status !== 401) {
            if (!aplicarErroresServidor(this.datos, errorApi(error))) {
              this.errorDatos.set(mensajeError(error));
            }
          }
        },
      });
  }

  cambiarPassword(): void {
    if (this.password.invalid || this.cambiandoPassword()) {
      this.password.markAllAsTouched();
      return;
    }
    this.cambiandoPassword.set(true);
    this.errorPassword.set(null);
    const { actual, nueva } = this.password.getRawValue();
    this.api.cambiarPassword(actual, nueva).subscribe({
      next: () => {
        this.cambiandoPassword.set(false);
        this.password.reset();
        this.notificaciones.exito('Contraseña cambiada.');
      },
      error: (error: HttpErrorResponse) => {
        this.cambiandoPassword.set(false);
        if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          if (!aplicarErroresServidor(this.password, errorApi(error))) {
            this.errorPassword.set(mensajeError(error));
          }
        }
      },
    });
  }

  /** Envía otra vez el enlace para confirmar el email: el nuevo, si hay un cambio pendiente (D-088). */
  enviarConfirmacion(): void {
    if (this.enviandoConfirmacion()) {
      return;
    }
    this.enviandoConfirmacion.set(true);
    this.api.reenviarConfirmacion().subscribe({
      next: ({ email }) => {
        this.enviandoConfirmacion.set(false);
        this.notificaciones.exito(`Te hemos enviado el enlace a ${email}.`);
      },
      error: (error: HttpErrorResponse) => {
        this.enviandoConfirmacion.set(false);
        if (error.status >= 400 && error.status < 500) {
          this.notificaciones.error(mensajeError(error));
        }
      },
    });
  }

  /** Descarta el email nuevo que esperaba su confirmación. */
  cancelarCambioEmail(): void {
    this.api.cancelarCambioEmail().subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.notificaciones.info('Cambio de email cancelado.');
      },
      error: (error: HttpErrorResponse) => {
        if (error.status >= 400 && error.status < 500) {
          this.notificaciones.error(mensajeError(error));
        }
      },
    });
  }

  desvincularGoogle(): void {
    abrirModalDesvincularGoogle(this.modales).subscribe((perfil) => {
      this.perfil.set(perfil);
      this.notificaciones.exito('Tu cuenta ya no está vinculada con Google.');
    });
  }

  /** Con un email nuevo, la contraseña actual pasa a ser obligatoria (D-086). */
  private comprobarEmail(): void {
    const actual = this.perfil()?.email ?? '';
    const cambia = this.datos.controls.email.value.trim().toLowerCase() !== actual.toLowerCase();
    const password = this.datos.controls.password;
    if (cambia !== this.cambiaEmail()) {
      this.cambiaEmail.set(cambia);
      password.setValidators(cambia ? Validators.required : null);
      if (!cambia) {
        password.reset('');
      }
      password.updateValueAndValidity();
    }
  }

  /**
   * Quien creó la cuenta con Google no tiene contraseña que sepa (D-085): con el enlace de recuperación, que llega a su
   * email, puede crear una.
   */
  enviarEnlacePassword(): void {
    const perfil = this.perfil();
    if (!perfil || this.enviandoEnlace()) {
      return;
    }
    this.enviandoEnlace.set(true);
    this.authApi.olvidoPassword(perfil.email).subscribe({
      next: () => {
        this.enviandoEnlace.set(false);
        this.notificaciones.exito(`Te hemos enviado el enlace a ${perfil.email}.`);
      },
      error: (error: HttpErrorResponse) => {
        this.enviandoEnlace.set(false);
        if (error.status >= 400 && error.status < 500) {
          this.notificaciones.error(mensajeError(error));
        }
      },
    });
  }

  private mostrar(perfil: Perfil): void {
    this.perfil.set(perfil);
    // Si lo ha confirmado en otro dispositivo, el aviso de la cabecera desaparece
    this.sesion.actualizarEmailVerificado(perfil.emailVerificado);
    this.datos.reset({
      nombre: perfil.nombre,
      apellido1: perfil.apellido1,
      apellido2: perfil.apellido2 ?? '',
      email: perfil.email,
      password: '',
    });
  }
}
