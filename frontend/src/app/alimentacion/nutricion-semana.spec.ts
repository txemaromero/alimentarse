import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { nutricionSemanaDePrueba } from '../../testing/datos';
import { NutricionSemana } from '../core/modelos';
import { FUENTE_BEDCA } from '../recetas/informacion-nutricional';
import { NutricionSemanaComponent } from './nutricion-semana';

registerLocaleData(localeEs);

describe('NutricionSemanaComponent', () => {
  let servidor: HttpTestingController;
  let fixture: ComponentFixture<NutricionSemanaComponent>;

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

  async function pintar(datos: NutricionSemana | null): Promise<HTMLElement> {
    fixture = TestBed.createComponent(NutricionSemanaComponent);
    await fixture.whenStable();
    const peticion = servidor.expectOne('/api/alimentacion/nutricion');
    if (datos) {
      peticion.flush(datos);
    } else {
      peticion.flush(null, { status: 500, statusText: 'Error' });
    }
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function celdas(html: HTMLElement, nutriente: string): string[] {
    const fila = [...html.querySelectorAll('tbody tr')].find(
      (tr) => tr.querySelector('th')!.textContent!.trim() === nutriente,
    )!;
    return [...fila.querySelectorAll('td')].map((td) =>
      td.textContent!.replace(/\s+/g, ' ').trim(),
    );
  }

  it('muestra la media al día, el porcentaje de la ingesta de referencia y el total', async () => {
    const html = await pintar(nutricionSemanaDePrueba());

    expect(html.querySelector('h2')!.textContent).toContain('Nutrientes en los últimos 7 días');
    expect(html.querySelector('.resumen')!.textContent!.replace(/\s+/g, ' ')).toContain(
      'Estimación con lo que has registrado: 2 de 3 consumos tienen datos',
    );
    expect(celdas(html, 'Energía')).toEqual(['116 kcal 485 kJ', '6 %', '811 kcal 3.394 kJ']);
    expect(celdas(html, 'Grasas')).toEqual(['1,6 g', '2 %', '11,1 g']);
    expect(celdas(html, 'Proteínas')).toEqual(['4,6 g', '9 %', '32,5 g']);
    expect(celdas(html, 'Sal')).toEqual(['0,05 g', '1 %', '0,37 g']);
    // La fibra no tiene ingesta de referencia
    expect(celdas(html, 'Fibra')[1]).toBe('–sin referencia');
    expect(html.querySelector('.fuente, a[href="https://www.bedca.net/"]')!.textContent).toContain(
      FUENTE_BEDCA,
    );
  });

  it('con un solo consumo lo dice en singular', async () => {
    const html = await pintar(nutricionSemanaDePrueba({ consumos: 1, estimados: 1 }));
    expect(html.querySelector('.resumen')!.textContent!.replace(/\s+/g, ' ')).toContain(
      '1 de 1 consumo tienen datos',
    );
  });

  it('sin consumos esta semana, o sin datos, lo explica en lugar de la tabla', async () => {
    let html = await pintar(
      nutricionSemanaDePrueba({
        consumos: 0,
        estimados: 0,
        total: null,
        mediaDiaria: null,
        referencia: null,
      }),
    );
    expect(html.querySelector('table')).toBeNull();
    expect(html.querySelector('.sin-datos')!.textContent).toContain(
      'No has consumido ninguna receta en los últimos 7 días.',
    );

    html = await pintar(
      nutricionSemanaDePrueba({ estimados: 0, total: null, mediaDiaria: null, referencia: null }),
    );
    expect(html.querySelector('.sin-datos')!.textContent).toContain(
      'Ninguna de las recetas que has comido en los últimos 7 días tiene información nutricional.',
    );
  });

  it('si falla, se puede reintentar', async () => {
    const html = await pintar(null);

    expect(html.textContent).toContain('No se han podido calcular los nutrientes.');
    html.querySelector<HTMLButtonElement>('.alert-danger button')!.click();
    servidor.expectOne('/api/alimentacion/nutricion').flush(nutricionSemanaDePrueba());
    await fixture.whenStable();
    expect(html.querySelector('table')).not.toBeNull();
  });
});
