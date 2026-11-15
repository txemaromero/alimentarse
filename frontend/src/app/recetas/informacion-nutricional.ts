import { DecimalPipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { Subscription } from 'rxjs';
import { IngredienteNutricion, NutricionReceta } from '../core/modelos';
import { RecetasApi } from './recetas-api';

/** Cita que pide BEDCA para usar sus datos (D-079). */
export const FUENTE_BEDCA =
  'AESAN/BEDCA Base de Datos Española de Composición de Alimentos v1.0 (2010)';

/**
 * Información nutricional estimada de una receta (D-079): por ración y de la receta entera, con la composición de BEDCA
 * de los ingredientes cuya cantidad se puede pasar a gramos. Debajo, cómo cuenta cada ingrediente y la fuente.
 */
@Component({
  selector: 'app-informacion-nutricional',
  imports: [DecimalPipe],
  templateUrl: './informacion-nutricional.html',
  styleUrl: './informacion-nutricional.scss',
})
export class InformacionNutricional {
  private readonly api = inject(RecetasApi);

  /** Id de la receta: solo se recalcula al abrir otra. */
  readonly receta = input.required<number>();

  readonly datos = signal<NutricionReceta | null>(null);
  readonly cargando = signal(false);
  readonly fallo = signal(false);

  /** Ingredientes que cuentan en la estimación. */
  readonly calculados = computed(
    () => this.datos()?.ingredientes.filter((i) => i.estado === 'CALCULADO').length ?? 0,
  );

  protected readonly fuente = FUENTE_BEDCA;

  private carga?: Subscription;

  constructor() {
    effect(() => {
      const id = this.receta();
      untracked(() => this.cargar(id));
    });
    inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
  }

  reintentar(): void {
    this.cargar(this.receta());
  }

  /** «1 ración», «4 raciones». */
  raciones(n: number): string {
    return `${n} ${n === 1 ? 'ración' : 'raciones'}`;
  }

  /** Lo que se dice de un ingrediente en «Cómo se calcula». */
  detalle(ingrediente: IngredienteNutricion): string {
    switch (ingrediente.estado) {
      case 'CALCULADO':
        return `cuenta como ${this.gramos(ingrediente.gramos ?? 0)} g de «${ingrediente.alimento}»`;
      case 'SIN_PESO':
        return 'no cuenta: su cantidad no se puede pasar a gramos';
      default:
        return 'no cuenta: no hay datos de su composición';
    }
  }

  private gramos(valor: number): string {
    return valor.toLocaleString('es', { maximumFractionDigits: 1 });
  }

  private cargar(id: number): void {
    this.carga?.unsubscribe();
    this.cargando.set(true);
    this.fallo.set(false);
    this.carga = this.api.nutricion(id).subscribe({
      next: (datos) => {
        this.datos.set(datos);
        this.cargando.set(false);
      },
      error: () => {
        this.datos.set(null);
        this.fallo.set(true);
        this.cargando.set(false);
      },
    });
  }
}
