import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { NutricionSemana } from '../core/modelos';
import { FUENTE_BEDCA } from '../recetas/informacion-nutricional';
import { ConsumosApi } from './consumos-api';

/**
 * Nutrientes de lo comido en los últimos 7 días (D-082): la media al día, su porcentaje de la ingesta de referencia de
 * un adulto medio y el total, con una ración de la receta de cada consumo.
 */
@Component({
  selector: 'app-nutricion-semana',
  imports: [DecimalPipe],
  templateUrl: './nutricion-semana.html',
  styles: `
    :host {
      display: block;
    }
    .tabla-semana {
      max-width: 44rem;
    }
    .tabla-semana td {
      white-space: nowrap;
    }
  `,
})
export class NutricionSemanaComponent implements OnInit {
  private readonly api = inject(ConsumosApi);

  readonly datos = signal<NutricionSemana | null>(null);
  readonly cargando = signal(true);
  readonly fallo = signal(false);

  protected readonly fuente = FUENTE_BEDCA;
  private carga?: Subscription;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.carga?.unsubscribe();
    this.cargando.set(true);
    this.fallo.set(false);
    this.carga = this.api.nutricionSemana().subscribe({
      next: (datos) => {
        this.datos.set(datos);
        this.cargando.set(false);
      },
      error: () => {
        this.fallo.set(true);
        this.cargando.set(false);
      },
    });
  }

  /** «1 consumo», «5 consumos». */
  consumos(n: number): string {
    return `${n} ${n === 1 ? 'consumo' : 'consumos'}`;
  }
}
