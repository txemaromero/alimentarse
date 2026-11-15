import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Logo } from './logo';

/** Pie de página: logo, páginas informativas y año. */
@Component({
  selector: 'app-pie',
  imports: [RouterLink, Logo],
  template: `
    <footer class="pie">
      <div
        class="container d-flex flex-column flex-md-row align-items-center gap-3 py-4 text-center text-md-start"
      >
        <app-logo />
        <nav aria-label="Información" class="flex-md-grow-1">
          <ul class="list-unstyled d-flex flex-wrap justify-content-center gap-3 m-0">
            <li><a routerLink="/ayuda">Ayuda</a></li>
            <li><a routerLink="/informacion-legal">Información legal</a></li>
            <li><a routerLink="/sobre-nosotros">Sobre nosotros</a></li>
          </ul>
        </nav>
        <p class="m-0 small">© {{ anio }} Toca — ¿Qué toca hoy?</p>
      </div>
    </footer>
  `,
  styles: `
    .pie {
      background-color: var(--toca-marron-oscuro);
      color: var(--toca-beige-claro);
    }
    app-logo {
      --logo-fondo: var(--toca-beige);
      --logo-color: var(--toca-marron-oscuro);
    }
    a {
      color: var(--toca-beige-claro);
      text-underline-offset: 0.2em;
    }
    a:hover {
      color: #fff;
    }
  `,
})
export class Pie {
  protected readonly anio = new Date().getFullYear();
}
