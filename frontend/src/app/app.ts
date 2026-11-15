import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AnunciosNgBootstrap } from './core/anuncios-ngb';
import { FocoAlNavegar } from './core/foco-al-navegar';
import { AvisoConexion } from './layout/aviso-conexion';
import { AvisoEmail } from './layout/aviso-email';
import { AvisoVersion } from './layout/aviso-version';
import { Avisos } from './layout/avisos';
import { Cabecera } from './layout/cabecera';
import { Pie } from './layout/pie';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Cabecera, AvisoConexion, AvisoEmail, Pie, Avisos, AvisoVersion],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  constructor() {
    inject(FocoAlNavegar).iniciar();
    inject(AnunciosNgBootstrap).iniciar();
  }

  /** Enlace «Saltar al contenido» para quien navega con teclado o lector de pantalla. */
  saltarAlContenido(contenido: HTMLElement): void {
    contenido.focus();
    contenido.scrollIntoView();
  }
}
