import { Component, computed, input } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faTemperatureFull,
  faTemperatureHalf,
  faTemperatureQuarter,
} from '@fortawesome/free-solid-svg-icons';
import { Dificultad } from '../core/modelos';

const ESTILO = {
  Fácil: { icono: faTemperatureQuarter, clase: 'dificultad-facil' },
  Media: { icono: faTemperatureHalf, clase: 'dificultad-media' },
  Difícil: { icono: faTemperatureFull, clase: 'dificultad-dificil' },
} as const;

/** Termómetro verde, ámbar o rojo con el texto de la dificultad (el color nunca va solo). */
@Component({
  selector: 'app-icono-dificultad',
  imports: [FaIconComponent],
  template: `
    <span [class]="estilo().clase" [title]="'Dificultad: ' + dificultad()">
      <fa-icon [icon]="estilo().icono" class="me-1" />{{ dificultad() }}
    </span>
  `,
  styles: `
    .dificultad-facil fa-icon {
      color: #2e7d32;
    }
    .dificultad-media fa-icon {
      color: #a35f00;
    }
    .dificultad-dificil fa-icon {
      color: #c62828;
    }
  `,
})
export class IconoDificultad {
  readonly dificultad = input.required<Dificultad>();
  protected readonly estilo = computed(() => ESTILO[this.dificultad()] ?? ESTILO.Media);
}
