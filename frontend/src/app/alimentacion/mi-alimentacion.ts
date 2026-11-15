import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCalendarDays, faClockRotateLeft, faUtensils } from '@fortawesome/free-solid-svg-icons';
import { RecetaResumen, ResumenAlimentacion } from '../core/modelos';
import { TarjetaReceta } from '../recetas/tarjeta-receta';
import { ConsumosApi } from './consumos-api';
import { IndicadorGrupoComponent } from './indicador-grupo';
import { NutricionSemanaComponent } from './nutricion-semana';

/** Formato de la fecha de un consumo: «dd/MM/yyyy 'a las' HH:mm» en hora local. */
export const FORMATO_CONSUMO = "dd/MM/yyyy 'a las' HH:mm";

/**
 * «Mi alimentación»: últimas recetas consumidas, las más consumidas, los ingredientes más usados, los indicadores
 * de los últimos 7 días (6.4) y sus nutrientes (D-082).
 */
@Component({
  selector: 'app-mi-alimentacion',
  imports: [
    RouterLink,
    DatePipe,
    FaIconComponent,
    TarjetaReceta,
    IndicadorGrupoComponent,
    NutricionSemanaComponent,
  ],
  templateUrl: './mi-alimentacion.html',
  styles: `
    .leyenda {
      white-space: nowrap;
    }
    .leyenda::before {
      content: '';
      display: inline-block;
      width: 0.75em;
      height: 0.75em;
      margin-right: 0.25em;
      border-radius: 50%;
    }
    .leyenda-verde::before {
      background-color: #2e7d32;
    }
    .leyenda-ambar::before {
      background-color: #c77800;
    }
    .leyenda-rojo::before {
      background-color: #c62828;
    }
    .puesto {
      display: inline-grid;
      place-items: center;
      flex: 0 0 2rem;
      height: 2rem;
      border-radius: 50%;
      background-color: var(--toca-beige-claro);
      color: var(--toca-marron-oscuro);
      font-weight: 700;
    }
  `,
})
export class MiAlimentacion implements OnInit {
  private readonly api = inject(ConsumosApi);

  readonly resumen = signal<ResumenAlimentacion | null>(null);
  readonly cargando = signal(true);
  readonly fallo = signal(false);

  protected readonly formato = FORMATO_CONSUMO;
  protected readonly iconos = {
    historico: faClockRotateLeft,
    planificador: faCalendarDays,
    vacio: faUtensils,
  };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.fallo.set(false);
    this.api.resumen().subscribe({
      next: (resumen) => {
        this.resumen.set(resumen);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.fallo.set(true);
      },
    });
  }

  /** ¿No hay ningún consumo todavía? */
  sinConsumos(resumen: ResumenAlimentacion): boolean {
    return resumen.ultimas.length === 0;
  }

  /** Un «me gusta» en una tarjeta cambia la receta en todas las secciones donde aparezca. */
  actualizar(receta: RecetaResumen): void {
    this.resumen.update(
      (r) =>
        r && {
          ...r,
          ultimas: r.ultimas.map((c) => (c.receta.id === receta.id ? { ...c, receta } : c)),
          masConsumidas: r.masConsumidas.map((m) =>
            m.receta.id === receta.id ? { ...m, receta } : m,
          ),
        },
    );
  }
}
