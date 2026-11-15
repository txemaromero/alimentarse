import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Estrellas } from './estrellas';

registerLocaleData(localeEs);

describe('Estrellas', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [{ provide: LOCALE_ID, useValue: 'es' }] });
  });

  async function pintar(nota: number, prefijo?: string) {
    const fixture = TestBed.createComponent(Estrellas);
    fixture.componentRef.setInput('nota', nota);
    if (prefijo !== undefined) {
      fixture.componentRef.setInput('prefijo', prefijo);
    }
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    return {
      // «fas:star» es una estrella llena; «far:star», vacía
      iconos: [...html.querySelectorAll('fa-icon svg')].map(
        (svg) => `${svg.getAttribute('data-prefix')}:${svg.getAttribute('data-icon')}`,
      ),
      texto: html.querySelector('.visually-hidden')!.textContent!.trim(),
      decorativas: html.querySelector('.estrellas')!.getAttribute('aria-hidden'),
    };
  }

  const LLENA = 'fas:star';
  const MEDIA = 'fas:star-half-stroke';
  const VACIA = 'far:star';

  it('redondea a medias estrellas', async () => {
    expect((await pintar(4)).iconos).toEqual([LLENA, LLENA, LLENA, LLENA, VACIA]);
    expect((await pintar(3.6)).iconos).toEqual([LLENA, LLENA, LLENA, MEDIA, VACIA]);
    expect((await pintar(3.2)).iconos).toEqual([LLENA, LLENA, LLENA, VACIA, VACIA]);
  });

  it('el lector de pantalla lee la nota con su prefijo, no las estrellas', async () => {
    const pintada = await pintar(4.5, 'Nota media: ');
    expect(pintada.texto).toBe('Nota media: 4,5 de 5');
    expect(pintada.decorativas).toBe('true');
    expect((await pintar(3)).texto).toBe('3 de 5');
  });
});
