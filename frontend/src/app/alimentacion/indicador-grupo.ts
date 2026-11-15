import { Component, computed, input } from '@angular/core';
import { ColorIndicador, IndicadorGrupo } from '../core/modelos';

const RADIO = 42;
export const CIRCUNFERENCIA = 2 * Math.PI * RADIO;

const ESTADO: Record<ColorIndicador, { trazo: string; texto: string }> = {
  VERDE: { trazo: '#2e7d32', texto: 'dentro de lo recomendado' },
  // Ámbar oscuro: contraste mayor de 3:1 sobre blanco (gráficos, WCAG 1.4.11)
  AMARILLO: { trazo: '#c77800', texto: 'cerca del límite' },
  ROJO: { trazo: '#c62828', texto: 'límite alcanzado' },
};

/** «Raciones C/L» o «Raciones C/∞» (6.4). */
export function textoRaciones7Dias(indicador: IndicadorGrupo): string {
  return `Raciones ${indicador.consumidas}/${indicador.limite ?? '∞'}`;
}

/**
 * Indicador circular de un grupo en los últimos 7 días (6.4): el progreso (C/L, con tope del 100 %; siempre lleno si
 * no hay límite) en verde, ámbar o rojo, con el nombre del grupo y las raciones. El estado también va en texto.
 */
@Component({
  selector: 'app-indicador-grupo',
  template: `
    @let i = indicador();
    <figure class="indicador m-0 text-center">
      <svg viewBox="0 0 100 100" class="circulo" aria-hidden="true">
        <circle cx="50" cy="50" [attr.r]="radio" class="fondo" />
        <circle
          cx="50"
          cy="50"
          [attr.r]="radio"
          class="progreso"
          [attr.stroke]="estado().trazo"
          [attr.stroke-dasharray]="circunferencia"
          [attr.stroke-dashoffset]="desplazamiento()"
        />
        <text x="50" y="50" class="cifra">{{ i.consumidas }}/{{ i.limite ?? '∞' }}</text>
      </svg>
      <figcaption>
        <strong class="d-block">{{ i.grupo.nombre }}</strong>
        <span class="small">{{ raciones() }}</span>
        <span class="visually-hidden">, {{ estado().texto }}</span>
      </figcaption>
    </figure>
  `,
  styles: `
    .circulo {
      width: 6.5rem;
      height: 6.5rem;
      margin-bottom: 0.5rem;
    }
    circle {
      fill: none;
      stroke-width: 10;
    }
    .fondo {
      stroke: #e9e2db;
    }
    .progreso {
      stroke-linecap: round;
      transform: rotate(-90deg);
      transform-origin: 50% 50%;
      transition: stroke-dashoffset 0.6s ease;
    }
    .cifra {
      font-size: 1.15rem;
      font-weight: 700;
      fill: #2a2a2a;
      text-anchor: middle;
      dominant-baseline: central;
    }
    @media (prefers-reduced-motion: reduce) {
      .progreso {
        transition: none;
      }
    }
  `,
})
export class IndicadorGrupoComponent {
  readonly indicador = input.required<IndicadorGrupo>();

  protected readonly radio = RADIO;
  protected readonly circunferencia = CIRCUNFERENCIA;
  readonly estado = computed(() => ESTADO[this.indicador().color]);
  readonly raciones = computed(() => textoRaciones7Dias(this.indicador()));
  /** Parte del círculo sin pintar: 0 = lleno. */
  readonly desplazamiento = computed(() => {
    const porcentaje = Math.min(Math.max(this.indicador().porcentaje, 0), 100);
    return CIRCUNFERENCIA * (1 - porcentaje / 100);
  });
}
