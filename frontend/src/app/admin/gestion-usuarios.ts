import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faMagnifyingGlass,
  faRotateLeft,
  faUserLock,
  faUserShield,
  faUsers,
} from '@fortawesome/free-solid-svg-icons';
import { crearListado } from '../compartido/listado';
import { Paginacion } from '../compartido/paginacion';
import { errorApi, mensajeError } from '../core/errores';
import { UsuarioAdmin } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { AdminApi } from './admin-api';
import { RequiereConexion } from '../compartido/requiere-conexion';

interface FiltroUsuarios {
  q?: string;
}

/** Nombre y apellidos en una línea. */
export function nombreCompleto(u: UsuarioAdmin): string {
  return [u.nombre, u.apellido1, u.apellido2].filter(Boolean).join(' ');
}

/**
 * Gestión de usuarios (solo ADMIN): buscador y tabla paginada con los interruptores de administrador y de bloqueo
 * (D-063).
 */
@Component({
  selector: 'app-gestion-usuarios',
  imports: [ReactiveFormsModule, NgTemplateOutlet, FaIconComponent, Paginacion, RequiereConexion],
  templateUrl: './gestion-usuarios.html',
  styles: `
    .interruptor {
      min-width: 2.75rem;
    }
    .min-w-0 {
      min-width: 0;
    }
    .interruptor.activo {
      color: #fff;
      background-color: var(--bs-primary);
      border-color: var(--bs-primary);
    }
    .interruptor.bloqueo.activo {
      background-color: var(--bs-danger);
      border-color: var(--bs-danger);
    }
  `,
})
export class GestionUsuarios implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly notificaciones = inject(Notificaciones);
  protected readonly sesion = inject(Sesion);

  readonly listado = crearListado<UsuarioAdmin, FiltroUsuarios>('admin-usuarios', (filtro, p) =>
    this.api.buscar(filtro.q, p),
  );
  readonly buscador = new FormGroup({ q: new FormControl('', { nonNullable: true }) });
  /** Id del usuario cuyo rol o bloqueo se está cambiando. */
  readonly cambiando = signal<string | null>(null);
  /** El servidor ha respondido 403: quien está conectado ya no es administrador (D-043). */
  readonly sinPermiso = signal(false);

  protected readonly nombreCompleto = nombreCompleto;
  protected readonly iconos = {
    buscar: faMagnifyingGlass,
    restablecer: faRotateLeft,
    admin: faUserShield,
    bloqueo: faUserLock,
    vacio: faUsers,
  };

  ngOnInit(): void {
    this.buscador.reset({ q: this.listado.filtro().q ?? '' });
    this.listado.iniciar();
  }

  buscar(): void {
    const q = this.buscador.controls.q.value.trim();
    this.listado.filtrar(q ? { q } : {});
  }

  restablecer(): void {
    this.buscador.reset({ q: '' });
    this.listado.filtrar({});
  }

  esYo(usuario: UsuarioAdmin): boolean {
    return usuario.id === this.sesion.usuario()?.id;
  }

  /** Por qué no se puede cambiar su rol, o null si se puede. */
  motivoSinCambioAdmin(usuario: UsuarioAdmin): string | null {
    if (this.esYo(usuario)) {
      return 'No puedes quitarte el rol a ti mismo';
    }
    return usuario.bloqueado && !usuario.admin
      ? 'Desbloquéalo antes de hacerlo administrador'
      : null;
  }

  /** Por qué no se le puede bloquear, o null si se puede. */
  motivoSinBloqueo(usuario: UsuarioAdmin): string | null {
    if (this.esYo(usuario)) {
      return 'No puedes bloquearte a ti mismo';
    }
    return usuario.admin ? 'Quítale antes el rol de administrador' : null;
  }

  alternarAdmin(usuario: UsuarioAdmin): void {
    if (this.cambiando() || this.motivoSinCambioAdmin(usuario)) {
      return;
    }
    this.cambiar(usuario, this.api.cambiarAdmin(usuario.id, !usuario.admin), (actualizado) =>
      actualizado.admin
        ? `${actualizado.id} ya es administrador. Tendrá que volver a iniciar sesión.`
        : `${actualizado.id} ya no es administrador.`,
    );
  }

  alternarBloqueo(usuario: UsuarioAdmin): void {
    if (this.cambiando() || this.motivoSinBloqueo(usuario)) {
      return;
    }
    this.cambiar(usuario, this.api.cambiarBloqueo(usuario.id, !usuario.bloqueado), (actualizado) =>
      actualizado.bloqueado
        ? `${actualizado.id} está bloqueado: ya no puede entrar y su sesión abierta se ha cerrado.`
        : `${actualizado.id} ya puede volver a entrar.`,
    );
  }

  private cambiar(
    usuario: UsuarioAdmin,
    peticion: Observable<UsuarioAdmin>,
    aviso: (actualizado: UsuarioAdmin) => string,
  ): void {
    this.cambiando.set(usuario.id);
    peticion.subscribe({
      next: (actualizado) => {
        this.cambiando.set(null);
        this.listado.reemplazar(actualizado, (a, b) => a.id === b.id);
        this.notificaciones.exito(aviso(actualizado));
      },
      error: (error: HttpErrorResponse) => {
        this.cambiando.set(null);
        if (error.status === 403) {
          this.sinPermiso.set(true);
        } else if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          this.notificaciones.error(mensajeError(error));
          if (errorApi(error)?.codigo === 'NO_ENCONTRADO') {
            this.listado.recargar();
          }
        }
      },
    });
  }
}
