import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { anuncioDistinto, mantenerAccesible } from './anunciador';

/** Id de la región viva que ng-bootstrap crea en el `<body>` para sus anuncios. */
export const REGION_NGB = 'ngb-live';

/** El anuncio en español, o `null` si no es uno de los textos en inglés de ng-bootstrap. */
export function traducirAnuncio(texto: string): string | null {
  if (texto === 'No results available') {
    return 'Ningún resultado';
  }
  const resultados = /^(\d+) results? available$/.exec(texto);
  if (!resultados) {
    return null;
  }
  return resultados[1] === '1' ? '1 resultado' : `${resultados[1]} resultados`;
}

/**
 * Corrige la región viva que usa el autocompletado de ng-bootstrap (D-075):
 *
 * - Traduce sus anuncios, que están fijos en inglés («3 results available») y no se pueden configurar: su servicio
 *   `Live` no es público. NVDA los leía con la voz española. Un `MutationObserver` cambia el texto en cuanto
 *   ng-bootstrap lo escribe, antes de que el navegador actualice el árbol de accesibilidad, así que el lector de
 *   pantalla solo recibe el texto en español. Un anuncio igual al anterior se distingue con `anuncioDistinto`.
 * - Impide que un modal la oculte (`mantenerAccesible`): la región cuelga de `<body>`, y NVDA solo anunciaba la
 *   primera búsqueda del modal del planificador, cuando la región aún no existía al abrirlo.
 */
@Injectable({ providedIn: 'root' })
export class AnunciosNgBootstrap {
  private readonly documento = inject(DOCUMENT);

  iniciar(): void {
    const existente = this.documento.getElementById(REGION_NGB);
    if (existente) {
      this.vigilar(existente);
    }
    // ng-bootstrap crea la región con el primer anuncio
    new MutationObserver((cambios) => {
      for (const cambio of cambios) {
        for (const nodo of Array.from(cambio.addedNodes)) {
          if (nodo instanceof HTMLElement && nodo.id === REGION_NGB) {
            this.vigilar(nodo);
          }
        }
      }
    }).observe(this.documento.body, { childList: true });
  }

  private vigilar(region: HTMLElement): void {
    mantenerAccesible(region);
    let anterior = '';
    new MutationObserver(() => {
      const traducido = traducirAnuncio(region.textContent ?? '');
      if (traducido !== null) {
        anterior = anuncioDistinto(traducido, anterior);
        region.textContent = anterior;
      }
    }).observe(region, { childList: true, characterData: true, subtree: true });
  }
}
