import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faChevronLeft, faChevronRight, faImage } from '@fortawesome/free-solid-svg-icons';
import { srcsetImagen, urlImagen } from '../compartido/imagenes';
import { Anunciador } from '../core/anunciador';
import { Foto } from '../core/modelos';

/**
 * La galería del detalle de una receta (D-083): la foto que se ve, con su descripción, las flechas para pasar de una
 * a otra (también con las teclas de flecha) y las miniaturas. Al imprimir, solo la foto que se ve.
 */
@Component({
  selector: 'app-galeria-fotos',
  imports: [FaIconComponent],
  templateUrl: './galeria-fotos.html',
  styleUrl: './galeria-fotos.scss',
  // Las flechas del teclado con el foco en las flechas o en las miniaturas
  host: { '(keydown)': 'tecla($event)' },
})
export class GaleriaFotos {
  private readonly anunciador = inject(Anunciador);

  readonly fotos = input.required<Foto[]>();
  /** El título de la receta, para el texto alternativo de las fotos sin descripción. */
  readonly titulo = input.required<string>();

  readonly indice = signal(0);
  readonly actual = computed(() => this.fotos()[this.indice()] ?? null);

  protected readonly urlImagen = urlImagen;
  protected readonly srcsetImagen = srcsetImagen;
  protected readonly iconos = {
    anterior: faChevronLeft,
    siguiente: faChevronRight,
    sinImagen: faImage,
  };

  constructor() {
    // Otra receta, u otra galería tras editarla: se empieza por la principal
    effect(() => {
      this.fotos();
      untracked(() => this.indice.set(0));
    });
  }

  /** La descripción, o «Tortilla: foto 2 de 3» (con una sola foto, «Foto de Tortilla»). */
  alt(foto: Foto, indice: number): string {
    if (foto.descripcion) {
      return foto.descripcion;
    }
    const total = this.fotos().length;
    return total === 1
      ? `Foto de ${this.titulo()}`
      : `${this.titulo()}: foto ${indice + 1} de ${total}`;
  }

  /** «Ver la foto 2 de 3: la tortilla cortada». */
  etiquetaMiniatura(foto: Foto, indice: number): string {
    const base = `Ver la foto ${indice + 1} de ${this.fotos().length}`;
    return foto.descripcion ? `${base}: ${foto.descripcion}` : base;
  }

  ver(indice: number): void {
    const total = this.fotos().length;
    if (total === 0) {
      return;
    }
    const nuevo = (indice + total) % total;
    this.indice.set(nuevo);
    const foto = this.fotos()[nuevo];
    this.anunciador.anunciar(
      `Foto ${nuevo + 1} de ${total}` + (foto.descripcion ? `: ${foto.descripcion}` : '.'),
    );
  }

  anterior(): void {
    this.ver(this.indice() - 1);
  }

  siguiente(): void {
    this.ver(this.indice() + 1);
  }

  /** Flechas izquierda y derecha con el foco dentro de la galería. */
  tecla(evento: KeyboardEvent): void {
    if (this.fotos().length < 2) {
      return;
    }
    if (evento.key === 'ArrowLeft') {
      evento.preventDefault();
      this.anterior();
    } else if (evento.key === 'ArrowRight') {
      evento.preventDefault();
      this.siguiente();
    }
  }
}
