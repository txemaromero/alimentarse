import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faHeart as faHeartVacio } from '@fortawesome/free-regular-svg-icons';
import {
  faClock,
  faEye,
  faEyeSlash,
  faHeart,
  faPenToSquare,
  faTrashCan,
  faUser,
  faUtensils,
} from '@fortawesome/free-solid-svg-icons';
import { srcsetImagen, urlImagen } from '../compartido/imagenes';
import { confirmar, OpcionesConfirmacion } from '../compartido/modal-confirmacion';
import { mensajeError } from '../core/errores';
import { RecetaResumen } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Estrellas } from './estrellas';
import { IconoDificultad } from './icono-dificultad';
import { RecetasApi } from './recetas-api';
import { RequiereConexion } from '../compartido/requiere-conexion';

/** Confirmación de «Eliminar receta», la misma en la tarjeta y en el detalle. */
export function confirmacionEliminarReceta(
  titulo: string,
  accion: () => Observable<void>,
): OpcionesConfirmacion<void> {
  return {
    titulo: 'Eliminar la receta',
    mensaje:
      `¿Seguro que quieres eliminar «${titulo}»? No se puede deshacer. Si alguien ya la ha consumido, ` +
      'seguirá apareciendo en su histórico, pero nadie podrá volver a abrirla.',
    aceptar: 'Sí, eliminar',
    accion,
  };
}

/**
 * Tarjeta de receta reutilizable: imagen, título, autor, tipo, tiempo, comensales, dificultad y «me gusta». En
 * `modoAutor` (Mis recetas) añade publicar/despublicar, editar y eliminar. Tras cada cambio emite la receta
 * actualizada; tras eliminarla, `eliminada`. Una receta eliminada (D-061) se muestra sin enlace ni «me gusta».
 */
@Component({
  selector: 'app-tarjeta-receta',
  imports: [RouterLink, DecimalPipe, FaIconComponent, IconoDificultad, RequiereConexion, Estrellas],
  templateUrl: './tarjeta-receta.html',
  styleUrl: './tarjeta-receta.scss',
})
export class TarjetaReceta {
  private readonly api = inject(RecetasApi);
  private readonly notificaciones = inject(Notificaciones);
  private readonly modales = inject(NgbModal);

  readonly receta = input.required<RecetaResumen>();
  readonly modoAutor = input(false);
  readonly cambio = output<RecetaResumen>();
  readonly eliminada = output<RecetaResumen>();

  readonly ocupado = signal(false);

  protected readonly urlImagen = urlImagen;
  protected readonly srcsetImagen = srcsetImagen;
  protected readonly iconos = {
    reloj: faClock,
    comensales: faUser,
    meGusta: faHeart,
    noMeGusta: faHeartVacio,
    publicada: faEye,
    noPublicada: faEyeSlash,
    editar: faPenToSquare,
    eliminar: faTrashCan,
    sinImagen: faUtensils,
  };

  alternarMeGusta(): void {
    const receta = this.receta();
    if (this.ocupado() || !receta.publicada || receta.oculta) {
      return;
    }
    this.ocupado.set(true);
    this.api.meGusta(receta.id, !receta.meGusta).subscribe({
      next: (likes) => {
        this.ocupado.set(false);
        this.cambio.emit({ ...receta, ...likes });
      },
      error: (error: HttpErrorResponse) => this.fallo(error),
    });
  }

  alternarPublicada(): void {
    const receta = this.receta();
    if (this.ocupado()) {
      return;
    }
    this.ocupado.set(true);
    this.api.publicar(receta.id, !receta.publicada).subscribe({
      next: (detalle) => {
        this.ocupado.set(false);
        this.notificaciones.exito(
          detalle.publicada
            ? `«${detalle.titulo}» ya está publicada.`
            : `«${detalle.titulo}» ya no está publicada.`,
        );
        this.cambio.emit({ ...receta, publicada: detalle.publicada });
      },
      error: (error: HttpErrorResponse) => this.fallo(error),
    });
  }

  eliminar(): void {
    const receta = this.receta();
    confirmar(
      this.modales,
      confirmacionEliminarReceta(receta.titulo, () => this.api.eliminar(receta.id)),
    ).subscribe(() => {
      this.notificaciones.exito(`«${receta.titulo}» se ha eliminado.`);
      this.eliminada.emit(receta);
    });
  }

  private fallo(error: HttpErrorResponse): void {
    this.ocupado.set(false);
    if (error.status >= 400 && error.status < 500 && error.status !== 401) {
      this.notificaciones.error(mensajeError(error));
    }
  }
}
