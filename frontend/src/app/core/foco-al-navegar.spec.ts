import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, RouterOutlet, provideRouter } from '@angular/router';
import { FocoAlNavegar } from './foco-al-navegar';

@Component({ template: '<h1>Recetas</h1><a href="#">Una receta</a>' })
class ConTitulo {}

@Component({ template: '<p>Cargando…</p>' })
class SinTitulo {}

/** Como app.html: el contenido de cada pantalla, dentro de `main#contenido`. */
@Component({
  imports: [RouterOutlet],
  template:
    '<a id="enlace" href="#">Menú</a><main id="contenido" tabindex="-1"><router-outlet /></main>',
})
class Anfitrion {}

describe('FocoAlNavegar', () => {
  let fixture: ComponentFixture<Anfitrion>;
  let router: Router;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'recetas', component: ConTitulo },
          { path: 'otra', component: ConTitulo },
          { path: 'cargando', component: SinTitulo },
        ]),
      ],
    });
    fixture = TestBed.createComponent(Anfitrion);
    router = TestBed.inject(Router);
    TestBed.inject(FocoAlNavegar).iniciar();
    await navegar('/recetas');
  });

  async function navegar(url: string) {
    (document.getElementById('enlace') as HTMLElement).focus();
    await router.navigateByUrl(url);
    await fixture.whenStable();
  }

  it('en la primera carga no mueve el foco', () => {
    expect(document.activeElement?.id).toBe('enlace');
  });

  it('al cambiar de pantalla, el foco pasa a su título', async () => {
    await navegar('/otra');

    const titulo = document.activeElement as HTMLElement;
    expect(titulo.tagName).toBe('H1');
    expect(titulo.getAttribute('tabindex')).toBe('-1');
  });

  it('si la pantalla aún no tiene título, al contenido principal', async () => {
    await navegar('/cargando');

    expect(document.activeElement?.id).toBe('contenido');
  });

  it('si solo cambia la consulta (filtros, página), el foco no se mueve', async () => {
    await navegar('/recetas?page=2');

    expect(document.activeElement?.id).toBe('enlace');
  });
});
