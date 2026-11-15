import { DatePipe, formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  Injector,
  LOCALE_ID,
  OnInit,
  afterNextRender,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faCartShopping,
  faCheck,
  faChevronLeft,
  faChevronRight,
  faPlus,
  faTriangleExclamation,
  faXmark,
} from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subscription } from 'rxjs';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { mensajeError } from '../core/errores';
import { ComidaPlanificada, Plan } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { IndicadorGrupoComponent } from './indicador-grupo';
import { abrirModalPlanificar } from './modal-planificar';
import { PlanApi } from './plan-api';
import {
  ClaveMomento,
  MOMENTOS,
  Momento,
  SEMANAS_MAXIMAS,
  claveHueco,
  diasDeLaSemana,
  finDelDia,
  instanteDe,
  momentoDe,
  semanaDeLaUrl,
} from './semana-plan';

/**
 * Planificador semanal (D-068): una comida y una cena por día, 7 días a partir de hoy, con el aviso de lo que superaría
 * su límite y los círculos de cómo quedaría la semana.
 */
@Component({
  selector: 'app-planificador',
  imports: [RouterLink, DatePipe, FaIconComponent, RequiereConexion, IndicadorGrupoComponent],
  templateUrl: './planificador.html',
  styles: `
    .momento {
      letter-spacing: 0.04em;
    }
  `,
})
export class Planificador implements OnInit {
  private readonly api = inject(PlanApi);
  private readonly modales = inject(NgbModal);
  private readonly notificaciones = inject(Notificaciones);
  private readonly locale = inject(LOCALE_ID);
  private readonly injector = inject(Injector);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  private readonly hoy = new Date();

  /** `?semana=`: la semana con la que se abre, para volver a ella desde la lista de la compra (D-076). */
  readonly semana = input(0, { transform: semanaDeLaUrl });
  /** La semana que se ve: 0 = la que empieza hoy. */
  readonly semanaVista = signal(0);
  readonly plan = signal<Plan | null>(null);
  readonly cargando = signal(true);
  readonly fallo = signal(false);
  /** Comida con una operación en curso. */
  readonly ocupada = signal<number | null>(null);

  readonly dias = computed(() => diasDeLaSemana(this.hoy, this.semanaVista()));
  readonly huecos = computed(() => {
    const huecos = new Map<string, ComidaPlanificada>();
    for (const comida of this.plan()?.comidas ?? []) {
      const fecha = new Date(comida.fecha);
      huecos.set(claveHueco(fecha, momentoDe(fecha)), comida);
    }
    return huecos;
  });

  protected readonly momentos = MOMENTOS;
  protected readonly semanasMaximas = SEMANAS_MAXIMAS;
  protected readonly iconos = {
    anterior: faChevronLeft,
    siguiente: faChevronRight,
    anadir: faPlus,
    comida: faCheck,
    quitar: faXmark,
    aviso: faTriangleExclamation,
    compra: faCartShopping,
  };

  private carga?: Subscription;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
  }

  ngOnInit(): void {
    this.semanaVista.set(this.semana());
    this.cargar();
  }

  /**
   * `enfocar`: id del hueco que recibe el foco al terminar. Tras planificar, quitar o comer, el botón pulsado
   * desaparece y el foco se perdería al principio de la página (D-075).
   */
  cargar(enfocar?: string): void {
    this.carga?.unsubscribe();
    this.cargando.set(true);
    this.fallo.set(false);
    const dias = this.dias();
    this.carga = this.api.obtener(dias[0], finDelDia(dias[dias.length - 1])).subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.cargando.set(false);
        if (enfocar) {
          afterNextRender(() => document.getElementById(enfocar)?.focus(), {
            injector: this.injector,
          });
        }
      },
      error: () => {
        this.cargando.set(false);
        this.fallo.set(true);
      },
    });
  }

  cambiarSemana(desplazamiento: number): void {
    this.semanaVista.update((semana) => semana + desplazamiento);
    // La semana queda en la URL, sin añadir una entrada al historial: «atrás» sigue saliendo del planificador
    void this.router.navigate([], {
      relativeTo: this.ruta,
      queryParams: { semana: this.semanaVista() || null },
      replaceUrl: true,
    });
    this.plan.set(null);
    this.cargar();
  }

  /** Id del hueco de un día y un momento: el `<li>` que recibe el foco tras cambiarlo. */
  idHueco(dia: Date, momento: ClaveMomento): string {
    return `hueco-${claveHueco(dia, momento)}`;
  }

  comidaEn(dia: Date, momento: ClaveMomento): ComidaPlanificada | undefined {
    return this.huecos().get(claveHueco(dia, momento));
  }

  esHoy(dia: Date): boolean {
    return dia.toDateString() === this.hoy.toDateString();
  }

  yaHaPasado(dia: Date, momento: Momento): boolean {
    return instanteDe(dia, momento).getTime() <= Date.now();
  }

  /** «martes 10 nov», para las etiquetas de los botones y el título del modal. */
  nombreDelDia(dia: Date): string {
    return formatDate(dia, 'EEEE d MMM', this.locale);
  }

  anadir(dia: Date, momento: Momento): void {
    const hueco = `${momento.etiqueta} del ${this.nombreDelDia(dia)}`;
    abrirModalPlanificar(this.modales, {
      titulo: hueco,
      fecha: instanteDe(dia, momento),
    }).subscribe((comida) => {
      this.notificaciones.exito(`«${comida.receta.titulo}» planificada: ${hueco.toLowerCase()}.`);
      this.cargar(this.idHueco(dia, momento.clave));
    });
  }

  quitar(comida: ComidaPlanificada): void {
    this.operar(comida, this.api.borrar(comida.id), () =>
      this.notificaciones.exito(`«${comida.receta.titulo}» quitada del planificador.`),
    );
  }

  consumir(comida: ComidaPlanificada): void {
    this.operar(comida, this.api.consumir(comida.id), () =>
      this.notificaciones.exito(`«${comida.receta.titulo}» añadida a tus consumos.`),
    );
  }

  /** Tras cualquier operación se vuelve a cargar: cambian los avisos y los círculos. */
  private operar<T>(comida: ComidaPlanificada, peticion: Observable<T>, alTerminar: () => void) {
    if (this.ocupada() !== null) {
      return;
    }
    this.ocupada.set(comida.id);
    const fecha = new Date(comida.fecha);
    const hueco = this.idHueco(fecha, momentoDe(fecha));
    peticion.subscribe({
      next: () => {
        this.ocupada.set(null);
        alTerminar();
        this.cargar(hueco);
      },
      error: (error: HttpErrorResponse) => {
        this.ocupada.set(null);
        if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          this.notificaciones.error(mensajeError(error));
          this.cargar();
        }
      },
    });
  }
}
