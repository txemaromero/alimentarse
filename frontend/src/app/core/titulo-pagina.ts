import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

export const NOMBRE_APLICACION = 'Alimentarse';

/** Título de la pestaña: «Página · Alimentarse», a partir del `title` de cada ruta. */
@Injectable()
export class TituloPagina extends TitleStrategy {
  private readonly titulo = inject(Title);

  override updateTitle(estado: RouterStateSnapshot): void {
    const pagina = this.buildTitle(estado);
    this.titulo.setTitle(pagina ? `${pagina} · ${NOMBRE_APLICACION}` : NOMBRE_APLICACION);
  }
}
