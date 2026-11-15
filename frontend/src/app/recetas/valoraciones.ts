import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  Injector,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faStar as faEstrellaVacia } from '@fortawesome/free-regular-svg-icons';
import { faStar } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subscription, map } from 'rxjs';
import { AdminApi } from '../admin/admin-api';
import { ErrorCampo } from '../compartido/error-campo';
import { confirmar } from '../compartido/modal-confirmacion';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { aplicarErroresServidor, errorApi, esInvalido, mensajeError } from '../core/errores';
import { RecetaDetalle, ResumenValoraciones, Valoracion } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { Estrellas } from './estrellas';
import { RecetasApi } from './recetas-api';

/** Longitud máxima del comentario, la misma que en el backend. */
export const MAXIMO_COMENTARIO = 1000;

/**
 * Valoraciones de una receta (D-077): la nota media, el formulario de la valoración propia (si no es la autora y la
 * receta está publicada) y la lista, de la más reciente a la más antigua, con «Ver más». La administración puede
 * ocultar una valoración o volver a mostrarla.
 */
@Component({
  selector: 'app-valoraciones',
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    FaIconComponent,
    ErrorCampo,
    RequiereConexion,
    Estrellas,
  ],
  templateUrl: './valoraciones.html',
  styleUrl: './valoraciones.scss',
})
export class Valoraciones {
  private readonly api = inject(RecetasApi);
  private readonly admin = inject(AdminApi);
  private readonly notificaciones = inject(Notificaciones);
  private readonly modales = inject(NgbModal);
  private readonly injector = inject(Injector);
  private readonly destruir = inject(DestroyRef);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly receta = input.required<RecetaDetalle>();
  /** Cómo queda la receta tras cambiar una valoración: la pantalla actualiza la media y la propia. */
  readonly cambio = output<ResumenValoraciones>();

  readonly esAdmin = inject(Sesion).esAdmin;
  readonly lista = signal<Valoracion[]>([]);
  readonly total = signal(0);
  readonly cargando = signal(false);
  readonly fallo = signal(false);
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);
  /** Valoración con una operación de moderación en curso. */
  readonly ocupada = signal<number | null>(null);

  readonly formulario = this.fb.group({
    nota: this.fb.control<number | null>(null, Validators.required),
    comentario: ['', Validators.maxLength(MAXIMO_COMENTARIO)],
  });
  readonly nota = this.formulario.controls.nota;
  readonly comentario = this.formulario.controls.comentario;
  readonly notaElegida = toSignal(this.nota.valueChanges.pipe(map((nota) => nota ?? 0)), {
    initialValue: 0,
  });
  readonly caracteres = toSignal(this.comentario.valueChanges.pipe(map((texto) => texto.length)), {
    initialValue: 0,
  });

  /** Quien no es su autor y puede verla, si está publicada y no oculta. */
  readonly puedeValorar = computed(() => {
    const receta = this.receta();
    return !receta.esAutor && receta.publicada && !receta.oculta;
  });

  protected readonly esInvalido = esInvalido;
  protected readonly notas = [1, 2, 3, 4, 5];
  protected readonly maximoComentario = MAXIMO_COMENTARIO;
  protected readonly iconos = { llena: faStar, vacia: faEstrellaVacia };

  /** Solo el id: un «me gusta» cambia la receta, pero no debe recargar la lista ni borrar lo escrito. */
  private readonly idReceta = computed(() => this.receta().id);
  private pagina = 0;
  private carga?: Subscription;

  constructor() {
    // Al abrir otra receta: su lista y su valoración propia
    effect(() => {
      const id = this.idReceta();
      untracked(() => {
        this.rellenar(this.receta().miValoracion);
        this.cargar(id, 0);
      });
    });
    this.destruir.onDestroy(() => this.carga?.unsubscribe());
  }

  /** «1 valoración», «12 valoraciones». */
  textoValoraciones(n: number): string {
    return `${n} ${n === 1 ? 'valoración' : 'valoraciones'}`;
  }

  verMas(): void {
    this.cargar(this.receta().id, this.pagina + 1);
  }

  reintentar(): void {
    this.cargar(this.receta().id, 0);
  }

  guardar(): void {
    if (this.formulario.invalid || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { nota, comentario } = this.formulario.getRawValue();
    this.api
      .valorar(this.receta().id, nota!, comentario.trim() || null)
      .pipe(takeUntilDestroyed(this.destruir))
      .subscribe({
        next: (resumen) => {
          this.enviando.set(false);
          this.actualizar(resumen);
          this.notificaciones.exito('Valoración guardada.');
        },
        error: (e: HttpErrorResponse) => {
          this.enviando.set(false);
          // Los 5xx y los fallos de red ya los avisa el interceptor
          if (
            e.status >= 400 &&
            e.status < 500 &&
            !aplicarErroresServidor(this.formulario, errorApi(e))
          ) {
            this.error.set(mensajeError(e));
          }
        },
      });
  }

  borrar(): void {
    const id = this.receta().id;
    confirmar(this.modales, {
      titulo: 'Borrar tu valoración',
      mensaje: 'Se borrarán tu nota y tu comentario de esta receta.',
      aceptar: 'Borrar',
      accion: () => this.api.borrarValoracion(id),
    }).subscribe((resumen) => {
      this.actualizar(resumen);
      this.notificaciones.exito('Valoración borrada.');
      // El botón pulsado desaparece: el foco pasa al formulario (D-075)
      afterNextRender(() => document.getElementById('tu-valoracion')?.focus(), {
        injector: this.injector,
      });
    });
  }

  /** Moderación: la oculta o la vuelve a mostrar, y la media se recalcula. */
  alternarOculta(valoracion: Valoracion): void {
    this.ocupada.set(valoracion.id);
    const oculta = !valoracion.oculta;
    this.admin
      .ocultarValoracion(valoracion.id, oculta)
      .pipe(takeUntilDestroyed(this.destruir))
      .subscribe({
        next: (actualizada) => {
          this.lista.update((lista) =>
            lista.map((v) => (v.id === actualizada.id ? actualizada : v)),
          );
          this.notificaciones.exito(
            oculta
              ? `Valoración de ${valoracion.usuario} oculta: ya no cuenta en la media.`
              : `Valoración de ${valoracion.usuario} visible de nuevo.`,
          );
          this.recargarMedia();
        },
        error: (e: HttpErrorResponse) => {
          this.ocupada.set(null);
          if (e.status >= 400 && e.status < 500) {
            this.notificaciones.error(mensajeError(e));
          }
        },
      });
  }

  private actualizar(resumen: ResumenValoraciones): void {
    this.cambio.emit(resumen);
    this.rellenar(resumen.mia);
    this.cargar(this.receta().id, 0);
  }

  /** La media cambia al ocultar o mostrar: se vuelve a pedir la receta. */
  private recargarMedia(): void {
    const receta = this.receta();
    this.api
      .detalle(receta.id)
      .pipe(takeUntilDestroyed(this.destruir))
      .subscribe({
        next: (detalle) => {
          this.ocupada.set(null);
          this.cambio.emit({
            nota: detalle.nota,
            valoraciones: detalle.valoraciones,
            mia: detalle.miValoracion,
          });
        },
        error: () => this.ocupada.set(null),
      });
  }

  private rellenar(mia: Valoracion | null): void {
    this.formulario.reset({ nota: mia?.nota ?? null, comentario: mia?.comentario ?? '' });
  }

  private cargar(receta: number, pagina: number): void {
    this.carga?.unsubscribe();
    this.cargando.set(true);
    this.fallo.set(false);
    this.carga = this.api.valoraciones(receta, pagina).subscribe({
      next: (resultado) => {
        this.cargando.set(false);
        this.pagina = pagina;
        this.total.set(resultado.totalElementos);
        this.lista.update((lista) => {
          const anteriores = pagina === 0 ? [] : lista;
          const nuevas = resultado.contenido.filter((v) => !anteriores.some((a) => a.id === v.id));
          return [...anteriores, ...nuevas];
        });
      },
      error: () => {
        this.cargando.set(false);
        this.fallo.set(true);
      },
    });
  }
}
