import { TestBed } from '@angular/core/testing';
import { AnunciosNgBootstrap, REGION_NGB, traducirAnuncio } from './anuncios-ngb';

describe('AnunciosNgBootstrap', () => {
  /** Deja que se ejecuten los MutationObserver (microtareas). */
  const observadores = () => new Promise((r) => setTimeout(r));

  afterEach(() => document.getElementById(REGION_NGB)?.remove());

  it('traduce los anuncios del autocompletado', () => {
    expect(traducirAnuncio('No results available')).toBe('Ningún resultado');
    expect(traducirAnuncio('1 result available')).toBe('1 resultado');
    expect(traducirAnuncio('12 results available')).toBe('12 resultados');
    expect(traducirAnuncio('3 resultados')).toBeNull();
    expect(traducirAnuncio('')).toBeNull();
  });

  it('cambia el texto en cuanto ng-bootstrap lo escribe, también si crea la región después', async () => {
    TestBed.inject(AnunciosNgBootstrap).iniciar();
    const region = document.createElement('div');
    region.id = REGION_NGB;
    document.body.appendChild(region);
    await observadores();

    region.textContent = '';
    region.textContent = '3 results available';
    await observadores();
    expect(region.textContent).toBe('3 resultados');

    region.textContent = 'No results available';
    await observadores();
    expect(region.textContent).toBe('Ningún resultado');
  });

  it('el mismo anuncio dos veces seguidas es un cambio de texto', async () => {
    TestBed.inject(AnunciosNgBootstrap).iniciar();
    const region = document.createElement('div');
    region.id = REGION_NGB;
    document.body.appendChild(region);
    await observadores();

    const textos: (string | null)[] = [];
    for (let i = 0; i < 3; i++) {
      region.textContent = '';
      region.textContent = '1 result available';
      await observadores();
      textos.push(region.textContent);
    }
    expect(textos).toEqual(['1 resultado', '1 resultado\u00A0', '1 resultado']);
  });

  it('vigila la región si ya existía al arrancar', async () => {
    const region = document.createElement('div');
    region.id = REGION_NGB;
    document.body.appendChild(region);
    TestBed.inject(AnunciosNgBootstrap).iniciar();

    region.textContent = '1 result available';
    await observadores();
    expect(region.textContent).toBe('1 resultado');
  });

  it('un modal no puede ocultar la región', async () => {
    TestBed.inject(AnunciosNgBootstrap).iniciar();
    const region = document.createElement('div');
    region.id = REGION_NGB;
    document.body.appendChild(region);
    await observadores();

    // Lo que hace NgbModal con todo lo que cuelga de <body> fuera de él
    region.setAttribute('aria-hidden', 'true');
    await observadores();
    expect(region.hasAttribute('aria-hidden')).toBe(false);
  });
});
