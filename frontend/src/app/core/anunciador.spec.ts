import { TestBed } from '@angular/core/testing';
import {
  Anunciador,
  REGION_CORTES,
  REGION_URGENTE,
  RETRASO_ANUNCIO,
  anuncioDistinto,
  mantenerAccesible,
} from './anunciador';

describe('Anunciador', () => {
  const esperar = (ms = 0) => new Promise((r) => setTimeout(r, ms));
  const cortes = () => document.getElementById(REGION_CORTES)!;
  const urgente = () => document.getElementById(REGION_URGENTE)!;

  it('un anuncio igual al anterior lleva un espacio de no separación al final', () => {
    expect(anuncioDistinto('1 resultado', '')).toBe('1 resultado');
    expect(anuncioDistinto('1 resultado', '1 resultado')).toBe('1 resultado\u00A0');
    expect(anuncioDistinto('1 resultado', '1 resultado\u00A0')).toBe('1 resultado');
    expect(anuncioDistinto('2 resultados', '1 resultado')).toBe('2 resultados');
  });

  it('crea las dos regiones vivas al arrancar, como hijas directas de <body>', () => {
    TestBed.inject(Anunciador);
    expect(document.querySelectorAll(`#${REGION_CORTES}`)).toHaveLength(1);
    expect(cortes().parentElement).toBe(document.body);
    expect(urgente().parentElement).toBe(document.body);
    expect(cortes().getAttribute('aria-live')).toBe('polite');
    expect(urgente().getAttribute('aria-live')).toBe('assertive');
    for (const region of [cortes(), urgente()]) {
      expect(region.getAttribute('aria-atomic')).toBe('true');
      expect(region.classList).toContain('visually-hidden');
      expect(region.textContent).toBe('');
    }
  });

  it('vacía la región y escribe el texto poco después; lo urgente va a la otra región', async () => {
    const anunciador = TestBed.inject(Anunciador);
    anunciador.anunciar('Receta guardada.');
    expect(cortes().textContent).toBe('');
    await esperar(RETRASO_ANUNCIO + 20);
    expect(cortes().textContent).toBe('Receta guardada.');

    anunciador.anunciar('No se ha podido guardar.', true);
    await esperar(RETRASO_ANUNCIO + 20);
    expect(urgente().textContent).toBe('No se ha podido guardar.');
    expect(cortes().textContent).toBe('Receta guardada.');
  });

  it('el mismo anuncio dos veces seguidas es un cambio de texto', async () => {
    const anunciador = TestBed.inject(Anunciador);
    const textos: (string | null)[] = [];
    for (let i = 0; i < 3; i++) {
      anunciador.anunciar('Añadida a tus consumos.');
      await esperar(RETRASO_ANUNCIO + 20);
      textos.push(cortes().textContent);
    }
    expect(textos).toEqual([
      'Añadida a tus consumos.',
      'Añadida a tus consumos.\u00A0',
      'Añadida a tus consumos.',
    ]);
  });

  it('un modal no puede ocultar las regiones', async () => {
    TestBed.inject(Anunciador);
    // Lo que hace NgbModal con todo lo que cuelga de <body> fuera de él
    cortes().setAttribute('aria-hidden', 'true');
    urgente().setAttribute('aria-hidden', 'true');
    await esperar();
    expect(cortes().hasAttribute('aria-hidden')).toBe(false);
    expect(urgente().hasAttribute('aria-hidden')).toBe(false);
  });

  it('mantenerAccesible quita también un aria-hidden que ya estuviera', () => {
    const region = document.createElement('div');
    region.setAttribute('aria-hidden', 'true');
    mantenerAccesible(region);
    expect(region.hasAttribute('aria-hidden')).toBe(false);
  });

  it('quita sus regiones al destruirse', () => {
    TestBed.inject(Anunciador).ngOnDestroy();
    expect(document.getElementById(REGION_CORTES)).toBeNull();
    expect(document.getElementById(REGION_URGENTE)).toBeNull();
  });
});
