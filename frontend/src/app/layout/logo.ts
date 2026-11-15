import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faUtensils } from '@fortawesome/free-solid-svg-icons';

/** Logo de la aplicación (cubiertos + «Toca»). Siempre lleva a Inicio. */
@Component({
  selector: 'app-logo',
  imports: [RouterLink, FaIconComponent],
  template: `
    <a routerLink="/" class="logo" title="Toca — ¿Qué toca hoy? Ir a Inicio">
      <span class="logo-icono"><fa-icon [icon]="icono" /></span>
      <span class="logo-texto">Toca</span>
      <span class="visually-hidden">, ir a Inicio</span>
    </a>
  `,
  styles: `
    .logo {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      color: inherit;
      text-decoration: none;
      font-weight: 800;
      letter-spacing: 0.02em;
    }
    .logo-icono {
      display: inline-grid;
      place-items: center;
      width: 2.25rem;
      height: 2.25rem;
      border-radius: 50%;
      background: var(--logo-fondo, var(--toca-marron-oscuro));
      color: var(--logo-color, var(--toca-beige-claro));
      font-size: 1rem;
    }
    .logo-texto {
      font-size: 1.4rem;
    }
  `,
})
export class Logo {
  protected readonly icono = faUtensils;
}
