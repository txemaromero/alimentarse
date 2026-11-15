import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  GRUPO_CARNE,
  GRUPO_HORTALIZAS,
  ingredienteDePrueba,
  nutricionSemanaDePrueba,
  recetaDePrueba,
} from '../../testing/datos';
import { IndicadorGrupo, ResumenAlimentacion } from '../core/modelos';
import { CIRCUNFERENCIA, IndicadorGrupoComponent, textoRaciones7Dias } from './indicador-grupo';
import { MiAlimentacion } from './mi-alimentacion';

registerLocaleData(localeEs);

/** 4 de noviembre de 2026, 13:30 en la hora local de quien ejecuta el test. */
const FECHA = new Date(2026, 10, 4, 13, 30).toISOString();

function indicador(cambios: Partial<IndicadorGrupo> = {}): IndicadorGrupo {
  return {
    grupo: GRUPO_CARNE,
    consumidas: 3,
    limite: 3,
    porcentaje: 100,
    color: 'ROJO',
    ...cambios,
  };
}

function resumenDePrueba(cambios: Partial<ResumenAlimentacion> = {}): ResumenAlimentacion {
  return {
    ultimas: [
      { id: 15, fecha: FECHA, receta: recetaDePrueba({ id: 1, titulo: 'Tortilla' }) },
      { id: 14, fecha: FECHA, receta: recetaDePrueba({ id: 2, titulo: 'Lentejas' }) },
    ],
    masConsumidas: [
      { receta: recetaDePrueba({ id: 2, titulo: 'Lentejas' }), veces: 3 },
      { receta: recetaDePrueba({ id: 1, titulo: 'Tortilla' }), veces: 1 },
    ],
    ingredientesMasUsados: [
      {
        ingrediente: ingredienteDePrueba({ id: 30, nombre: 'Pollo', grupo: GRUPO_CARNE }),
        veces: 4,
      },
      { ingrediente: ingredienteDePrueba(), veces: 1 },
    ],
    grupos7Dias: [
      indicador(),
      indicador({
        grupo: GRUPO_HORTALIZAS,
        consumidas: 2,
        limite: null,
        porcentaje: 100,
        color: 'VERDE',
      }),
    ],
    ...cambios,
  };
}

describe('IndicadorGrupo', () => {
  async function crear(datos: IndicadorGrupo) {
    const fixture = TestBed.createComponent(IndicadorGrupoComponent);
    fixture.componentRef.setInput('indicador', datos);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('muestra las raciones C/L o C/∞', () => {
    expect(textoRaciones7Dias(indicador())).toBe('Raciones 3/3');
    expect(textoRaciones7Dias(indicador({ consumidas: 2, limite: null }))).toBe('Raciones 2/∞');
  });

  it('pinta el progreso con el color y el estado también en texto', async () => {
    const html = await crear(
      indicador({ consumidas: 3, limite: 4, porcentaje: 75, color: 'AMARILLO' }),
    );
    const progreso = html.querySelector('.progreso') as SVGCircleElement;
    expect(progreso.getAttribute('stroke')).toBe('#c77800');
    expect(Number(progreso.getAttribute('stroke-dashoffset'))).toBeCloseTo(CIRCUNFERENCIA * 0.25);
    expect(html.querySelector('figcaption')?.textContent).toContain('Carne');
    expect(html.querySelector('figcaption')?.textContent).toContain('Raciones 3/4');
    expect(html.querySelector('.visually-hidden')?.textContent).toContain('cerca del límite');
  });

  it('un grupo sin límite se ve lleno y en verde', async () => {
    const html = await crear(indicador({ limite: null, color: 'VERDE', porcentaje: 100 }));
    const progreso = html.querySelector('.progreso') as SVGCircleElement;
    expect(Number(progreso.getAttribute('stroke-dashoffset'))).toBe(0);
    expect(progreso.getAttribute('stroke')).toBe('#2e7d32');
    expect(html.querySelector('.cifra')?.textContent).toBe('3/∞');
  });
});

describe('MiAlimentacion', () => {
  let fixture: ComponentFixture<MiAlimentacion>;
  let servidor: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'es' },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MiAlimentacion);
    await fixture.whenStable();
  });

  afterEach(() => servidor.verify());

  async function responder(resumen: ResumenAlimentacion) {
    servidor.expectOne('/api/alimentacion/resumen').flush(resumen);
    await fixture.whenStable();
    // Con consumos, la sección de nutrientes pide los suyos (D-082)
    for (const nutricion of servidor.match('/api/alimentacion/nutricion')) {
      nutricion.flush(nutricionSemanaDePrueba());
    }
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('muestra las últimas consumidas con su fecha y el enlace al histórico', async () => {
    const html = await responder(resumenDePrueba());
    const ultimas = html.querySelector('[aria-labelledby="titulo-ultimas"]')!;
    expect(ultimas.querySelectorAll('app-tarjeta-receta').length).toBe(2);
    expect(ultimas.querySelector('.consumida')?.textContent).toContain(
      'Receta consumida el 04/11/2026 a las 13:30',
    );
    expect(ultimas.querySelector('a[href="/mi-alimentacion/historico"]')?.textContent).toContain(
      'Ver histórico completo',
    );
  });

  it('muestra las más consumidas, los ingredientes más usados y los indicadores', async () => {
    const html = await responder(resumenDePrueba());

    const masConsumidas = html.querySelector('[aria-labelledby="titulo-mas-consumidas"]')!;
    expect(masConsumidas.querySelectorAll('.badge.text-bg-primary')[0].textContent).toContain(
      '3 veces',
    );
    expect(masConsumidas.querySelectorAll('.badge.text-bg-primary')[1].textContent).toContain(
      '1 vez',
    );

    const ingredientes = html.querySelector('[aria-labelledby="titulo-ingredientes"] ol')!;
    expect(ingredientes.children[0].textContent).toContain('Pollo');
    expect(ingredientes.children[0].textContent).toContain('En 4 consumos');
    expect(ingredientes.children[1].textContent).toContain('En 1 consumo');

    expect(html.querySelectorAll('app-indicador-grupo').length).toBe(2);
    // Los nutrientes de la semana van al final (D-082)
    expect(
      html.querySelector('[aria-labelledby="titulo-semana"] ~ app-nutricion-semana'),
    ).not.toBeNull();
  });

  it('sin consumos invita a registrar el primero', async () => {
    const html = await responder(
      resumenDePrueba({
        ultimas: [],
        masConsumidas: [],
        ingredientesMasUsados: [],
        grupos7Dias: [],
      }),
    );
    expect(html.textContent).toContain('Todavía no has registrado ninguna receta consumida.');
    expect(html.querySelector('app-indicador-grupo')).toBeNull();
    expect(html.querySelector('app-nutricion-semana')).toBeNull();
  });

  it('sin consumos en la última semana lo indica', async () => {
    const html = await responder(resumenDePrueba({ grupos7Dias: [] }));
    expect(html.textContent).toContain('No has consumido ninguna receta en los últimos 7 días.');
  });

  it('si falla permite reintentar', async () => {
    servidor
      .expectOne('/api/alimentacion/resumen')
      .flush(null, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    (html.querySelector('.alert-danger button') as HTMLButtonElement).click();
    await responder(resumenDePrueba());
    expect(html.querySelector('.alert-danger')).toBeNull();
  });

  it('un «me gusta» actualiza la receta en todas las secciones', async () => {
    await responder(resumenDePrueba());
    fixture.componentInstance.actualizar(
      recetaDePrueba({ id: 2, titulo: 'Lentejas', likes: 9, meGusta: true }),
    );
    const r = fixture.componentInstance.resumen()!;
    expect(r.ultimas[1].receta.likes).toBe(9);
    expect(r.masConsumidas[0].receta.likes).toBe(9);
  });
});
