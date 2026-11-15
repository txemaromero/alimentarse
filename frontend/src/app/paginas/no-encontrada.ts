import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faBowlRice } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-no-encontrada',
  imports: [RouterLink, FaIconComponent],
  template: `
    <section class="container py-5 text-center animate__animated animate__fadeIn">
      <fa-icon [icon]="icono" size="3x" class="text-secondary" />
      <h1 class="titulo-seccion">Página no encontrada</h1>
      <p class="lead">Lo sentimos, aquí no hay nada en el plato.</p>
      <a routerLink="/" class="btn btn-primary mt-2">Volver a Inicio</a>
    </section>
  `,
})
export class NoEncontrada {
  protected readonly icono = faBowlRice;
}
