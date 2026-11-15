import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faBars, faRightFromBracket, faUser } from '@fortawesome/free-solid-svg-icons';
import {
  NgbCollapse,
  NgbDropdown,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
} from '@ng-bootstrap/ng-bootstrap';
import { filter } from 'rxjs';
import { AuthApi } from '../core/auth-api';
import { AvisosPush } from '../core/avisos-push';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { Logo } from './logo';

/** Barra de navegación: menú principal, menú de administración y menú del usuario. */
@Component({
  selector: 'app-cabecera',
  imports: [
    RouterLink,
    RouterLinkActive,
    FaIconComponent,
    NgbCollapse,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
    Logo,
  ],
  templateUrl: './cabecera.html',
  styleUrl: './cabecera.scss',
})
export class Cabecera {
  protected readonly sesion = inject(Sesion);
  private readonly router = inject(Router);
  private readonly notificaciones = inject(Notificaciones);
  private readonly auth = inject(AuthApi);
  private readonly avisos = inject(AvisosPush);

  protected readonly plegado = signal(true);
  protected readonly iconos = { menu: faBars, usuario: faUser, salir: faRightFromBracket };

  constructor() {
    // En móvil, el menú se cierra al navegar
    this.router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe(() => this.plegado.set(true));
  }

  cerrarSesion(): void {
    const terminar = () => {
      this.sesion.cerrar();
      this.notificaciones.info('Has cerrado la sesión. ¡Hasta pronto!');
      this.router.navigateByUrl('/login');
    };
    // Primero los avisos de este navegador, que necesitan la sesión (D-078). Aunque algo falle (sin conexión), la
    // sesión se cierra en este dispositivo
    void this.avisos
      .alCerrarSesion()
      .then(() => this.auth.logout().subscribe({ next: terminar, error: terminar }));
  }
}
