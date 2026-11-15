import { DOCUMENT, DatePipe, Location, formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  DestroyRef,
  LOCALE_ID,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faHeart as faHeartVacio } from '@fortawesome/free-regular-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import {
  faArrowLeft,
  faBan,
  faClock,
  faEye,
  faEyeSlash,
  faHeart,
  faPenToSquare,
  faPlus,
  faPlay,
  faPrint,
  faTrashCan,
  faUser,
} from '@fortawesome/free-solid-svg-icons';
import { Subscription } from 'rxjs';
import { ConsumosApi } from '../alimentacion/consumos-api';
import { ErrorCampo } from '../compartido/error-campo';
import { ahoraLocal, noFutura, aInstanteUtc } from '../compartido/fechas';
import { esInvalido, mensajeError } from '../core/errores';
import { avisoImpacto } from '../alimentacion/recomendacion';
import { ImpactoGrupo, RecetaDetalle, ResumenValoraciones } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { NOMBRE_APLICACION } from '../core/titulo-pagina';
import { IconoDificultad } from './icono-dificultad';
import { RecetasApi } from './recetas-api';
import { InformacionNutricional } from './informacion-nutricional';
import { GaleriaFotos } from './galeria-fotos';
import { Valoraciones } from './valoraciones';
import { RequiereConexion } from '../compartido/requiere-conexion';
import { confirmar } from '../compartido/modal-confirmacion';
import { confirmacionEliminarReceta } from './tarjeta-receta';
import { abrirModalOcultarReceta } from '../admin/modal-ocultar-receta';
import { AdminApi } from '../admin/admin-api';
import { Sesion } from '../core/sesion';

/** «Como máximo N veces a la semana» o «Sin límite semanal» (6.1). */
export function textoMaxSemanal(maxSemanal: number | null): string {
  if (maxSemanal === null) {
    return 'Sin límite semanal';
  }
  return maxSemanal === 1
    ? 'Como máximo 1 vez a la semana'
    : `Como máximo ${maxSemanal} veces a la semana`;
}

@Component({
  selector: 'app-detalle-receta',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    FaIconComponent,
    DatePipe,
    IconoDificultad,
    ErrorCampo,
    RequiereConexion,
    Valoraciones,
    InformacionNutricional,
    GaleriaFotos,
  ],
  templateUrl: './detalle-receta.html',
  styleUrl: './detalle-receta.scss',
})
export class DetalleReceta {
  private readonly api = inject(RecetasApi);
  private readonly consumos = inject(ConsumosApi);
  private readonly notificaciones = inject(Notificaciones);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly documento = inject(DOCUMENT);
  private readonly titulo = inject(Title);
  private readonly locale = inject(LOCALE_ID);
  private readonly modales = inject(NgbModal);
  private readonly admin = inject(AdminApi);
  protected readonly sesion = inject(Sesion);

  /** Parámetro `:id` de la ruta. */
  readonly id = input.required<string>();

  readonly receta = signal<RecetaDetalle | null>(null);
  readonly cargando = signal(true);
  readonly noEncontrada = signal(false);
  readonly fallo = signal(false);
  readonly ocupado = signal(false);
  readonly registrando = signal(false);
  /** Aviso previo a consumir (D-067); vacío si no hay grupos con límite o la petición falla. */
  readonly impacto = signal<ImpactoGrupo[]>([]);

  readonly fecha = new FormControl(ahoraLocal(), {
    nonNullable: true,
    validators: [Validators.required, noFutura],
  });
  /** Con [formGroup] el envío lo gestiona Angular (ngSubmit) y no recarga la página. */
  readonly formularioConsumo = new FormGroup({ fecha: this.fecha });
  readonly maxFecha = signal(ahoraLocal());

  readonly maxSemanal = computed(() => textoMaxSemanal(this.receta()?.maxSemanal ?? null));

  protected readonly esInvalido = esInvalido;
  protected readonly avisoImpacto = avisoImpacto;
  protected readonly iconos = {
    volver: faArrowLeft,
    reloj: faClock,
    comensales: faUser,
    meGusta: faHeart,
    noMeGusta: faHeartVacio,
    publicar: faEye,
    despublicar: faEyeSlash,
    editar: faPenToSquare,
    eliminar: faTrashCan,
    ocultar: faBan,
    anadir: faPlus,
    imprimir: faPrint,
    video: faPlay,
  };

  private carga?: Subscription;

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.cargar(id));
    });
    inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
  }

  cargar(id = this.id()): void {
    this.carga?.unsubscribe();
    this.cargando.set(true);
    this.noEncontrada.set(false);
    this.fallo.set(false);
    const numero = Number(id);
    if (!Number.isInteger(numero) || numero < 1) {
      this.cargando.set(false);
      this.noEncontrada.set(true);
      return;
    }
    this.carga = this.api.detalle(numero).subscribe({
      next: (receta) => {
        this.receta.set(receta);
        this.cargando.set(false);
        this.titulo.setTitle(`${receta.titulo} · ${NOMBRE_APLICACION}`);
        this.cargarImpacto(receta);
      },
      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        this.receta.set(null);
        if (error.status === 404 || error.status === 400) {
          this.noEncontrada.set(true);
          this.titulo.setTitle(`Receta no encontrada · ${NOMBRE_APLICACION}`);
        } else {
          this.fallo.set(true);
        }
      },
    });
  }

  /**
   * Una receta oculta por la administración (D-063) solo la puede comer su autor: el administrador que la ve para
   * moderarla no tiene «¿La has comido?», porque el servidor no le dejaría registrarla ni calcular el impacto.
   */
  sePuedeComer(receta: RecetaDetalle): boolean {
    return !receta.oculta || receta.esAutor;
  }

  /** Solo es información adicional: si falla, se puede consumir igual. */
  private cargarImpacto(receta: RecetaDetalle): void {
    if (!this.sePuedeComer(receta)) {
      this.impacto.set([]);
      return;
    }
    this.api.impacto(receta.id).subscribe({
      next: (impacto) => this.impacto.set(impacto),
      error: () => this.impacto.set([]),
    });
  }

  /** Tras cambiar una valoración: la nota media, cuántas hay y la propia (D-077). */
  actualizarValoraciones(resumen: ResumenValoraciones): void {
    const receta = this.receta();
    if (receta) {
      this.receta.set({
        ...receta,
        nota: resumen.nota,
        valoraciones: resumen.valoraciones,
        miValoracion: resumen.mia,
      });
    }
  }

  /**
   * El diálogo de impresión del navegador, que también guarda en PDF (D-076). Los estilos de impresión dejan la receta
   * sola: sin menú, sin botones y con los ingredientes antes de las instrucciones.
   */
  /** «YouTube» o «Vimeo», según el enlace del vídeo (D-083). */
  plataformaVideo(enlace: string): string {
    return /vimeo\.com/i.test(enlace) ? 'Vimeo' : 'YouTube';
  }

  imprimir(): void {
    this.documento.defaultView?.print();
  }

  volver(): void {
    if (this.router.lastSuccessfulNavigation()?.previousNavigation) {
      this.location.back();
    } else {
      this.router.navigateByUrl('/recetas');
    }
  }

  alternarMeGusta(): void {
    const receta = this.receta();
    if (!receta || this.ocupado() || !receta.publicada || receta.oculta) {
      return;
    }
    this.ocupado.set(true);
    this.api.meGusta(receta.id, !receta.meGusta).subscribe({
      next: (likes) => {
        this.ocupado.set(false);
        this.receta.set({ ...receta, ...likes });
      },
      error: (error: HttpErrorResponse) => this.fallo4xx(error),
    });
  }

  alternarPublicada(): void {
    const receta = this.receta();
    if (!receta || this.ocupado()) {
      return;
    }
    this.ocupado.set(true);
    this.api.publicar(receta.id, !receta.publicada).subscribe({
      next: (actualizada) => {
        this.ocupado.set(false);
        this.receta.set(actualizada);
        this.notificaciones.exito(
          actualizada.publicada
            ? 'Receta publicada: ya la pueden ver todos los usuarios.'
            : 'La receta ya no está publicada: solo la ves tú.',
        );
      },
      error: (error: HttpErrorResponse) => this.fallo4xx(error),
    });
  }

  /** Moderación (D-063): ocultarla pide el motivo en un modal; volver a mostrarla es directo. */
  alternarOculta(): void {
    const receta = this.receta();
    if (!receta || this.ocupado()) {
      return;
    }
    if (!receta.oculta) {
      abrirModalOcultarReceta(this.modales, receta).subscribe((oculta) => {
        this.receta.set(oculta);
        this.notificaciones.exito('Receta oculta: ya solo la ven su autor y los administradores.');
      });
      return;
    }
    this.ocupado.set(true);
    this.admin.ocultarReceta(receta.id, false).subscribe({
      next: (visible) => {
        this.ocupado.set(false);
        this.receta.set(visible);
        this.cargarImpacto(visible);
        this.notificaciones.exito('La receta vuelve a estar visible.');
      },
      error: (error: HttpErrorResponse) => this.fallo4xx(error),
    });
  }

  /** Solo el autor (D-061). Tras eliminarla vuelve a «Mis recetas». */
  eliminar(): void {
    const receta = this.receta();
    if (!receta || this.ocupado()) {
      return;
    }
    confirmar(
      this.modales,
      confirmacionEliminarReceta(receta.titulo, () => this.api.eliminar(receta.id)),
    ).subscribe(() => {
      this.notificaciones.exito(`«${receta.titulo}» se ha eliminado.`);
      this.router.navigateByUrl('/mis-recetas');
    });
  }

  /** «Añadir a consumidos» (6.7) con la fecha y hora elegidas, convertidas a UTC. */
  anadirAConsumidos(): void {
    const receta = this.receta();
    this.fecha.updateValueAndValidity();
    if (!receta || !this.sePuedeComer(receta) || this.fecha.invalid || this.registrando()) {
      this.fecha.markAsTouched();
      return;
    }
    this.registrando.set(true);
    this.consumos.registrar(receta.id, aInstanteUtc(this.fecha.value)).subscribe({
      next: (consumo) => {
        this.registrando.set(false);
        const cuando = formatDate(consumo.fecha, "dd/MM/yyyy 'a las' HH:mm", this.locale);
        this.notificaciones.exito(`Añadida a tus consumos el ${cuando}.`);
        this.reiniciarFecha();
        this.cargarImpacto(receta);
      },
      error: (error: HttpErrorResponse) => {
        this.registrando.set(false);
        if (error.status >= 400 && error.status < 500 && error.status !== 401) {
          this.notificaciones.error(mensajeError(error));
        }
      },
    });
  }

  /** El selector no deja elegir una fecha posterior a «ahora» (que avanza mientras la página está abierta). */
  actualizarMaximo(): void {
    this.maxFecha.set(ahoraLocal());
  }

  reiniciarFecha(): void {
    const ahora = ahoraLocal();
    this.maxFecha.set(ahora);
    this.fecha.reset(ahora);
  }

  private fallo4xx(error: HttpErrorResponse): void {
    this.ocupado.set(false);
    if (error.status >= 400 && error.status < 500 && error.status !== 401) {
      this.notificaciones.error(mensajeError(error));
    }
  }
}
