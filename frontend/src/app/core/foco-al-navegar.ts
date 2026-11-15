import { DOCUMENT } from '@angular/common';
import { EnvironmentInjector, Injectable, afterNextRender, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

/**
 * Al cambiar de pantalla, lleva el foco a su título (`h1`) o, si aún no lo hay, al contenido principal (D-075).
 *
 * En una aplicación de una sola página el navegador no carga una página nueva: sin esto, el foco se queda en el enlace
 * pulsado (o se pierde, si ese enlace desaparece) y un lector de pantalla no anuncia nada. No se hace en la primera
 * carga, que el navegador y el lector ya tratan como una página nueva, ni cuando solo cambian la consulta o el ancla.
 */
@Injectable({ providedIn: 'root' })
export class FocoAlNavegar {
  private readonly router = inject(Router);
  private readonly injector = inject(EnvironmentInjector);
  private readonly documento = inject(DOCUMENT);
  private rutaAnterior: string | null = null;

  iniciar(): void {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        const ruta = e.urlAfterRedirects.split(/[?#]/)[0];
        const cambia = this.rutaAnterior !== null && ruta !== this.rutaAnterior;
        this.rutaAnterior = ruta;
        if (cambia) {
          afterNextRender(() => this.enfocar(), { injector: this.injector });
        }
      });
  }

  private enfocar(): void {
    const contenido = this.documento.getElementById('contenido');
    const titulo = contenido?.querySelector<HTMLElement>('h1');
    if (titulo) {
      // Un título no recibe el foco con Tab: solo así, desde el código
      titulo.setAttribute('tabindex', '-1');
      titulo.focus({ preventScroll: true });
    } else {
      contenido?.focus({ preventScroll: true });
    }
  }
}
