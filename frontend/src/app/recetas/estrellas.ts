import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faStar as faEstrellaVacia } from '@fortawesome/free-regular-svg-icons';
import { faStar, faStarHalfStroke } from '@fortawesome/free-solid-svg-icons';

/**
 * Una nota de 0 a 5 con estrellas, redondeada a medias (D-077). Las estrellas son decorativas: el lector de pantalla
 * lee el texto oculto, «{prefijo}4,5 de 5{sufijo}». Lo que se vea al lado con la misma información debe llevar
 * `aria-hidden`: si no, el lector lo leería pegado al texto oculto (D-075).
 */
@Component({
  selector: 'app-estrellas',
  imports: [FaIconComponent, DecimalPipe],
  template: `
    <span class="estrellas text-nowrap" aria-hidden="true">
      @for (icono of iconos(); track $index) {
        <fa-icon [icon]="icono" />
      }
    </span>
    <span class="visually-hidden"
      >{{ prefijo() }}{{ nota() | number: '1.0-1' }} de 5{{ sufijo() }}</span
    >
  `,
  styles: `
    // Dorado con un contraste de unos 4,7:1 sobre blanco: más del 3:1 que pide un gráfico (WCAG 1.4.11)
    .estrellas {
      color: #9a6a00;
    }
  `,
})
export class Estrellas {
  readonly nota = input.required<number>();
  /** Delante de la nota en el texto oculto, por ejemplo «Nota media: ». */
  readonly prefijo = input('');
  /** Detrás, por ejemplo «, 12 valoraciones». */
  readonly sufijo = input('');

  protected readonly iconos = computed(() => {
    const nota = Math.round(this.nota() * 2) / 2;
    return [1, 2, 3, 4, 5].map((i) =>
      nota >= i ? faStar : nota >= i - 0.5 ? faStarHalfStroke : faEstrellaVacia,
    );
  });
}
