import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { NutricionReceta } from '../core/modelos';
import { nutricionDePrueba } from '../../testing/datos';
import { FUENTE_BEDCA, InformacionNutricional } from './informacion-nutricional';

registerLocaleData(localeEs);

describe('InformacionNutricional', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<InformacionNutricional>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'es' },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  async function pintar(datos: NutricionReceta | null, receta = 5): Promise<HTMLElement> {
    fixture = TestBed.createComponent(InformacionNutricional);
    fixture.componentRef.setInput('receta', receta);
    await fixture.whenStable();
    const peticion = servidor.expectOne(`/api/recetas/${receta}/nutricion`);
    if (datos) {
      peticion.flush(datos);
    } else {
      peticion.flush(null, { status: 404, statusText: 'Not Found' });
    }
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function fila(html: HTMLElement, nutriente: string): string[] {
    const filas = [...html.querySelectorAll('tbody tr')];
    const encontrada = filas.find(
      (tr) => tr.querySelector('th')!.textContent!.trim() === nutriente,
    )!;
    return [...encontrada.querySelectorAll('td')].map((td) =>
      td.textContent!.replace(/\s+/g, ' ').trim(),
    );
  }

  it('muestra los valores por ración y de la receta entera, con la fuente', async () => {
    const html = await pintar(nutricionDePrueba());

    expect(html.querySelector('h2')!.textContent).toContain('Información nutricional');
    expect(html.textContent).toContain('Estimación orientativa con 2 de 4 ingredientes.');
    expect(html.querySelector('thead')!.textContent!.replace(/\s+/g, ' ')).toContain(
      'Receta (2 raciones)',
    );
    expect(fila(html, 'Energía')).toEqual(['406 kcal 1.697 kJ', '811 kcal 3.394 kJ']);
    expect(fila(html, 'Grasas')).toEqual(['5,6 g', '11,1 g']);
    expect(fila(html, 'de las cuales saturadas')).toEqual(['1,6 g', '3,1 g']);
    expect(fila(html, 'Hidratos de carbono')).toEqual(['71,5 g', '143 g']);
    expect(fila(html, 'Proteínas')).toEqual(['16,3 g', '32,5 g']);
    expect(fila(html, 'Sal')).toEqual(['0,18 g', '0,37 g']);
    expect(html.querySelector('caption')!.classList).toContain('visually-hidden');

    const enlace = html.querySelector<HTMLAnchorElement>('.fuente a')!;
    expect(enlace.href).toBe('https://www.bedca.net/');
    expect(enlace.rel).toBe('noopener');
    expect(enlace.textContent).toContain(FUENTE_BEDCA);
  });

  it('explica cómo cuenta cada ingrediente', async () => {
    const html = await pintar(nutricionDePrueba());

    const ingredientes = [...html.querySelectorAll('details li')].map((li) =>
      li.textContent!.replace(/\s+/g, ' ').trim(),
    );
    expect(ingredientes).toEqual([
      'Harina (200 gramos): cuenta como 200 g de «Harina de trigo»',
      'Huevos (2 unidades): cuenta como 100 g de «Huevo de gallina fresco»',
      'Levadura (1 sobre): no cuenta: no hay datos de su composición',
      'Sal (1 pizca): no cuenta: su cantidad no se puede pasar a gramos',
    ]);
    expect(html.querySelectorAll('details li.text-body-secondary').length).toBe(2);
  });

  it('con una sola ración dice «1 ración»', async () => {
    const html = await pintar(nutricionDePrueba({ comensales: 1 }));

    expect(html.querySelector('thead')!.textContent!.replace(/\s+/g, ' ')).toContain(
      'Receta (1 ración)',
    );
  });

  it('si ningún ingrediente cuenta, lo dice en lugar de la tabla', async () => {
    const html = await pintar(nutricionDePrueba({ total: null, porRacion: null }));

    expect(html.querySelector('table')).toBeNull();
    expect(html.querySelector('.sin-estimacion')!.textContent).toContain('No se puede estimar');
    expect(html.querySelector('.fuente')).not.toBeNull();
  });

  it('si falla, se puede reintentar', async () => {
    const html = await pintar(null);

    expect(html.textContent).toContain('No se ha podido calcular.');
    html.querySelector<HTMLButtonElement>('button')!.click();
    await fixture.whenStable();
    expect(html.querySelector('[role="status"]')).not.toBeNull();
    servidor.expectOne('/api/recetas/5/nutricion').flush(nutricionDePrueba());
    await fixture.whenStable();
    expect(html.querySelector('table')).not.toBeNull();
  });

  it('al cambiar de receta, pide la nueva', async () => {
    await pintar(nutricionDePrueba());

    fixture.componentRef.setInput('receta', 6);
    await fixture.whenStable();
    servidor.expectOne('/api/recetas/6/nutricion').flush(nutricionDePrueba());
  });
});
