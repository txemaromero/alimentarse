import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Router, provideRouter } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { aceptarConfirmacion } from '../../testing/confirmacion';
import {
  detalleDePrueba,
  nutricionDePrueba,
  paginaDe,
  valoracionDePrueba,
} from '../../testing/datos';
import { aValorLocal } from '../compartido/fechas';
import { ImpactoGrupo, IndicadorGrupo, RecetaDetalle } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { loginDePrueba } from '../../testing/jwt';
import { DetalleReceta, textoMaxSemanal } from './detalle-receta';

registerLocaleData(localeEs);

describe('DetalleReceta', () => {
  let fixture: ComponentFixture<DetalleReceta>;
  let servidor: HttpTestingController;

  beforeEach(() => {
    // La sesión se guarda en localStorage: sin esto, la de un test pasaría al siguiente
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'es' },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  async function crear(receta: RecetaDetalle | null, estado = 200, impacto: ImpactoGrupo[] = []) {
    fixture = TestBed.createComponent(DetalleReceta);
    fixture.componentRef.setInput('id', String(receta?.id ?? 99));
    await fixture.whenStable();
    const peticion = servidor.expectOne(`/api/recetas/${receta?.id ?? 99}`);
    if (receta) {
      peticion.flush(receta);
      // Una receta oculta solo la come su autor: a los demás no se les pide el impacto
      if (!receta.oculta || receta.esAutor) {
        servidor.expectOne(`/api/recetas/${receta.id}/impacto`).flush(impacto);
      }
    } else {
      peticion.flush(
        { codigo: 'NO_ENCONTRADO', mensaje: 'No existe', errores: {} },
        { status: estado, statusText: 'Not Found' },
      );
    }
    await fixture.whenStable();
    // Al pintar la receta se piden sus valoraciones (D-077) y su información nutricional (D-079)
    for (const valoraciones of servidor.match((r) => r.url.endsWith('/valoraciones'))) {
      valoraciones.flush(paginaDe([]));
    }
    for (const nutricion of servidor.match((r) => r.url.endsWith('/nutricion'))) {
      nutricion.flush(nutricionDePrueba());
    }
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function indicador(nombre: string, consumidas: number, limite: number): IndicadorGrupo {
    return {
      grupo: { id: consumidas * 10 + limite, nombre, racionesSemana: limite },
      consumidas,
      limite,
      porcentaje: 0,
      color: 'VERDE',
    };
  }

  it('avisa de cómo quedarían las raciones antes de consumir (D-067)', async () => {
    const html = await crear(detalleDePrueba(), 200, [
      { antes: indicador('Carne', 2, 3), despues: indicador('Carne', 3, 3) },
      { antes: indicador('Dulces', 1, 1), despues: indicador('Dulces', 2, 1) },
      { antes: indicador('Pasta', 0, 4), despues: indicador('Pasta', 1, 4) },
    ]);
    const filas = [...html.querySelectorAll('.impacto li')].map((li) =>
      (li.textContent ?? '').replace(/\s+/g, ' ').trim(),
    );
    expect(filas).toEqual([
      'Carne: de 2/3 a 3/3 llegarás al límite',
      'Dulces: de 1/1 a 2/1 te pasarás del límite',
      'Pasta: de 0/4 a 1/4',
    ]);
  });

  it('sin grupos con límite no muestra el aviso', async () => {
    const html = await crear(detalleDePrueba());
    expect(html.querySelector('.impacto')).toBeNull();
  });

  it('describe el máximo semanal', () => {
    expect(textoMaxSemanal(null)).toBe('Sin límite semanal');
    expect(textoMaxSemanal(1)).toBe('Como máximo 1 vez a la semana');
    expect(textoMaxSemanal(3)).toBe('Como máximo 3 veces a la semana');
  });

  it('muestra la receta con ingredientes, grupos, recomendación e instrucciones', async () => {
    // Mediodía UTC: es el mismo día en cualquier zona horaria de Europa y América
    const html = await crear(detalleDePrueba({ fechaCreacion: '2026-10-06T12:00:00Z' }));

    expect(html.querySelector('h1')?.textContent).toContain('Tortilla de patata');
    expect(html.querySelector('.ingredientes')?.textContent).toContain('Patata – 600 gramos');
    expect(Array.from(html.querySelectorAll('.chip')).map((c) => c.textContent?.trim())).toEqual([
      'Carne',
      'Hortalizas',
    ]);
    expect(html.textContent).toContain('Como máximo 3 veces a la semana');
    expect(html.querySelectorAll('.instrucciones li').length).toBe(2);
    expect(html.textContent).toContain('6 de octubre de 2026');
    expect(TestBed.inject(Title).getTitle()).toBe('Tortilla de patata · Alimentarse');
  });

  it('se imprime o se guarda en PDF con el diálogo del navegador (D-076)', async () => {
    const imprimir = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    const html = await crear(detalleDePrueba());
    const boton = [...html.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent!.includes('Imprimir o guardar en PDF'),
    )!;
    expect(boton.closest('.d-print-none')).not.toBeNull();
    boton.click();
    expect(imprimir).toHaveBeenCalled();
    imprimir.mockRestore();
  });

  it('muestra la galería y, si hay vídeo, un enlace que se abre en otra pestaña (D-083)', async () => {
    let html = await crear(detalleDePrueba({ video: 'https://vimeo.com/123' }));
    expect(html.querySelector('app-galeria-fotos img')!.getAttribute('alt')).toBe(
      'Foto de Tortilla de patata',
    );
    const enlace = html.querySelector<HTMLAnchorElement>('a.enlace-video')!;
    expect(enlace.textContent!.replace(/\s+/g, ' ').trim()).toBe(
      'Ver el vídeo en Vimeo (se abre en otra pestaña)',
    );
    expect(enlace.href).toBe('https://vimeo.com/123');
    expect(enlace.target).toBe('_blank');
    expect(enlace.rel).toBe('noopener noreferrer');
    expect(fixture.componentInstance.plataformaVideo('https://youtu.be/x')).toBe('YouTube');

    html = await crear(detalleDePrueba({ id: 2, fotos: [], imagen: null }));
    expect(html.querySelector('a.enlace-video')).toBeNull();
    expect(html.querySelector('app-galeria-fotos [role="img"]')).not.toBeNull();
  });

  it('la información nutricional va en el lateral, tras los ingredientes (D-079)', async () => {
    const html = await crear(detalleDePrueba());
    const tarjetas = [...html.querySelectorAll('aside .card')];
    const nutricion = html.querySelector('aside app-informacion-nutricional')!;

    expect(tarjetas.indexOf(nutricion.closest('.card')!)).toBe(1);
    expect(nutricion.textContent).toContain('406 kcal');
  });

  it('las valoraciones van tras las instrucciones y no se imprimen (D-077)', async () => {
    const html = await crear(detalleDePrueba({ nota: 4, valoraciones: 1 }));
    const seccion = html.querySelector('app-valoraciones')!;
    expect(seccion.classList).toContain('d-print-none');
    expect(html.querySelector('.cuerpo app-valoraciones')).not.toBeNull();

    const mia = valoracionDePrueba({ usuario: 'lucia', nota: 5 });
    fixture.componentInstance.actualizarValoraciones({ nota: 4.5, valoraciones: 2, mia });
    expect(fixture.componentInstance.receta()).toEqual(
      expect.objectContaining({ nota: 4.5, valoraciones: 2, miValoracion: mia }),
    );
  });

  it('a quien no es el autor no le ofrece publicar ni editar', async () => {
    const html = await crear(detalleDePrueba());
    expect(html.textContent).not.toContain('Editar');
  });

  it('el autor puede publicar su receta', async () => {
    const html = await crear(detalleDePrueba({ esAutor: true, publicada: false }));
    const boton = Array.from(html.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Publicar',
    )!;
    boton.click();
    servidor
      .expectOne('/api/recetas/1/publicada')
      .flush(detalleDePrueba({ esAutor: true, publicada: true }));
    await fixture.whenStable();

    expect(html.textContent).toContain('Dejar de publicar');
    expect(TestBed.inject(Notificaciones).avisos()[0].tipo).toBe('exito');
  });

  it('el autor puede eliminar su receta y vuelve a «Mis recetas»', async () => {
    const html = await crear(detalleDePrueba({ esAutor: true }));
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const modal = aceptarConfirmacion();

    Array.from(html.querySelectorAll('button'))
      .find((b) => b.textContent?.trim() === 'Eliminar')!
      .click();
    modal.aceptar();
    const peticion = servidor.expectOne('/api/recetas/1');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(navegar).toHaveBeenCalledWith('/mis-recetas');
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toContain('se ha eliminado');
  });

  it('el autor de una receta oculta ve el motivo y no puede publicarla', async () => {
    const html = await crear(
      detalleDePrueba({
        esAutor: true,
        publicada: false,
        oculta: true,
        motivoOculta: 'Publicidad encubierta',
      }),
    );
    expect(html.querySelector('.alert-danger')?.textContent).toContain(
      'Motivo: Publicidad encubierta',
    );
    const publicar = Array.from(html.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Publicar',
    )!;
    expect(publicar.disabled).toBe(true);
    // Su autor sí puede seguir apuntando que la ha comido
    expect(html.textContent).toContain('¿La has comido?');
  });

  it('un administrador oculta una receta con el motivo del modal', async () => {
    TestBed.inject(Sesion).iniciar(loginDePrueba(['USER', 'ADMIN']));
    const html = await crear(detalleDePrueba());
    const oculta = detalleDePrueba({ oculta: true, motivoOculta: 'Spam' });
    vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
      componentInstance: {},
      result: Promise.resolve(oculta),
    } as never);

    Array.from(html.querySelectorAll('.moderacion button'))
      .find((b) => b.textContent?.includes('Ocultar'))!
      .dispatchEvent(new Event('click'));
    await fixture.whenStable();

    expect(html.textContent).toContain('Oculta por la administración');
    expect(html.textContent).toContain('Motivo: Spam');
    expect(html.querySelector('.moderacion button')?.textContent).toContain('Volver a mostrar');
    // Oculta, el administrador ya no puede apuntarla como comida: el servidor no le dejaría
    expect(html.textContent).not.toContain('¿La has comido?');
  });

  it('un administrador vuelve a mostrar una receta oculta', async () => {
    TestBed.inject(Sesion).iniciar(loginDePrueba(['USER', 'ADMIN']));
    const html = await crear(detalleDePrueba({ oculta: true, motivoOculta: 'Spam' }));
    // Una receta ajena oculta no se puede comer: ni el formulario ni la petición del impacto
    expect(html.textContent).not.toContain('¿La has comido?');
    fixture.componentInstance.anadirAConsumidos();
    servidor.expectNone('/api/consumos');

    (html.querySelector('.moderacion button') as HTMLButtonElement).click();
    const peticion = servidor.expectOne('/api/admin/recetas/1/oculta');
    expect(peticion.request.body).toEqual({ oculta: false, motivo: null });
    peticion.flush(detalleDePrueba());
    servidor.expectOne('/api/recetas/1/impacto').flush([]);
    await fixture.whenStable();

    expect(html.querySelector('.alert-danger')).toBeNull();
    expect(html.textContent).toContain('¿La has comido?');
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toContain('vuelve a estar visible');
  });

  it('sin rol de administrador no hay moderación', async () => {
    const html = await crear(detalleDePrueba());
    expect(html.querySelector('.moderacion')).toBeNull();
  });

  it('a quien no es el autor no le ofrece eliminar', async () => {
    const html = await crear(detalleDePrueba());
    expect(html.querySelector('.btn-outline-danger')).toBeNull();
  });

  it('una receta inexistente o ajena sin publicar muestra «no encontrada»', async () => {
    const html = await crear(null, 404);
    expect(html.querySelector('h1')?.textContent).toContain('Receta no encontrada');
  });

  it('añade a consumidos la fecha elegida convertida a UTC', async () => {
    const html = await crear(detalleDePrueba());
    const componente = fixture.componentInstance;
    const fecha = new Date(2026, 10, 4, 13, 30);
    componente.fecha.setValue(aValorLocal(fecha));

    componente.anadirAConsumidos();

    const peticion = servidor.expectOne('/api/consumos');
    expect(peticion.request.body).toEqual({ receta: 1, fecha: fecha.toISOString() });
    peticion.flush({ id: 7, fecha: fecha.toISOString(), receta: detalleDePrueba() });
    // El aviso se actualiza: ya cuenta esta ración
    servidor.expectOne('/api/recetas/1/impacto').flush([]);

    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe(
      'Añadida a tus consumos el 04/11/2026 a las 13:30.',
    );
    expect(html.querySelector('#consumo-fecha')).toBeTruthy();
  });

  it('el formulario de consumo se envía con Angular, sin recargar la página', async () => {
    const html = await crear(detalleDePrueba());
    const formulario = html.querySelector('aside form') as HTMLFormElement;
    const envio = new Event('submit', { cancelable: true });

    formulario.dispatchEvent(envio);

    expect(envio.defaultPrevented).toBe(true);
    servidor
      .expectOne('/api/consumos')
      .flush({ id: 7, fecha: new Date().toISOString(), receta: detalleDePrueba() });
    servidor.expectOne('/api/recetas/1/impacto').flush([]);
  });

  it('no deja añadir una fecha futura', async () => {
    await crear(detalleDePrueba());
    const componente = fixture.componentInstance;
    componente.fecha.setValue(aValorLocal(new Date(Date.now() + 3 * 3600_000)));

    componente.anadirAConsumidos();

    servidor.expectNone('/api/consumos');
    expect(componente.fecha.hasError('futura')).toBe(true);
  });

  it('da «me gusta» desde el detalle', async () => {
    const html = await crear(detalleDePrueba());
    (html.querySelector('.boton-me-gusta') as HTMLButtonElement).click();
    servidor.expectOne('/api/recetas/1/like').flush({ likes: 4, meGusta: true });
    await fixture.whenStable();
    expect(html.textContent).toContain('4 personas le han dado');
  });
});
