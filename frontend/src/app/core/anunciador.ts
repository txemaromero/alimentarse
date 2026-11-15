import { DOCUMENT } from '@angular/common';
import { Injectable, OnDestroy, inject } from '@angular/core';

/** Lo que tarda en escribirse el texto tras vaciar la región. */
export const RETRASO_ANUNCIO = 100;

/** Ids de las dos regiones vivas de la aplicación. */
export const REGION_CORTES = 'anuncios-corteses';
export const REGION_URGENTE = 'anuncios-urgentes';

/**
 * El texto de un anuncio, de forma que no sea igual al anterior: si se repite, lleva al final un espacio de no
 * separación, que el lector de pantalla no pronuncia (D-075).
 *
 * Es una precaución: vaciar la región antes de escribir podría no bastar si el navegador agrupa los dos cambios, porque
 * para él el texto no habría cambiado.
 */
export function anuncioDistinto(texto: string, anterior: string): string {
  return texto === anterior ? `${texto}\u00A0` : texto;
}

/**
 * Impide que un modal oculte una región viva (D-075). Al abrirse, `NgbModal` pone `aria-hidden="true"` a todo lo que
 * cuelga de `<body>` fuera de él, y no lo quita hasta que termina la animación de cierre. Lo que se anunciara entretanto
 * no llegaba al lector de pantalla: los resultados del autocompletado dentro de un modal o el aviso que se muestra al
 * guardarlo. El `MutationObserver` quita el atributo antes de que el navegador actualice el árbol de accesibilidad.
 */
export function mantenerAccesible(region: HTMLElement): void {
  region.removeAttribute('aria-hidden');
  new MutationObserver(() => {
    if (region.hasAttribute('aria-hidden')) {
      region.removeAttribute('aria-hidden');
    }
  }).observe(region, { attributes: true, attributeFilter: ['aria-hidden'] });
}

/**
 * Anuncia textos a los lectores de pantalla sin mover el foco (D-075). Lo usa `Avisos` para cada aviso.
 *
 * Tiene dos regiones vivas, una cortés y otra urgente para los errores, creadas al arrancar: un lector de pantalla no
 * anuncia una región que aparece a la vez que su texto. Son hijas directas de `<body>`, fuera de `app-root`, para que
 * un modal no las oculte (`mantenerAccesible`).
 */
@Injectable({ providedIn: 'root' })
export class Anunciador implements OnDestroy {
  private readonly documento = inject(DOCUMENT);
  private readonly regiones = {
    cortes: this.crear(REGION_CORTES, 'polite'),
    urgente: this.crear(REGION_URGENTE, 'assertive'),
  };
  private readonly anteriores = { cortes: '', urgente: '' };

  anunciar(texto: string, urgente = false): void {
    const clave = urgente ? 'urgente' : 'cortes';
    const region = this.regiones[clave];
    const anuncio = anuncioDistinto(texto, this.anteriores[clave]);
    this.anteriores[clave] = anuncio;
    region.textContent = '';
    setTimeout(() => (region.textContent = anuncio), RETRASO_ANUNCIO);
  }

  ngOnDestroy(): void {
    this.regiones.cortes.remove();
    this.regiones.urgente.remove();
  }

  private crear(id: string, cortesia: 'polite' | 'assertive'): HTMLElement {
    const region = this.documento.createElement('div');
    region.id = id;
    region.className = 'visually-hidden';
    region.setAttribute('aria-live', cortesia);
    region.setAttribute('aria-atomic', 'true');
    this.documento.body.appendChild(region);
    mantenerAccesible(region);
    return region;
  }
}
