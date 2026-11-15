import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { paginaDe, recetaDePrueba } from '../../testing/datos';
import { aValorLocal } from '../compartido/fechas';
import { Consumo } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Historico } from './historico';
import { ModalConsumo } from './modal-consumo';

registerLocaleData(localeEs);

const FECHA = new Date(2026, 10, 4, 13, 30);

function consumoDePrueba(cambios: Partial<Consumo> = {}): Consumo {
  return { id: 15, fecha: FECHA.toISOString(), receta: recetaDePrueba(), ...cambios };
}

describe('histórico de consumos', () => {
  let servidor: HttpTestingController;
  let activo: { close: ReturnType<typeof vi.fn>; dismiss: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    activo = { close: vi.fn(), dismiss: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'es' },
        { provide: NgbActiveModal, useValue: activo },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  describe('Historico', () => {
    async function crear() {
      const fixture = TestBed.createComponent(Historico);
      await fixture.whenStable();
      servidor
        .expectOne('/api/consumos?page=0&size=9')
        .flush(
          paginaDe([
            consumoDePrueba(),
            consumoDePrueba({ id: 14, receta: recetaDePrueba({ id: 2, titulo: 'Lentejas' }) }),
          ]),
        );
      await fixture.whenStable();
      return fixture;
    }

    it('lista los consumos con su fecha', async () => {
      const html = (await crear()).nativeElement as HTMLElement;
      expect(html.querySelectorAll('app-tarjeta-receta').length).toBe(2);
      expect(html.querySelector('.consumida')?.textContent).toContain(
        'Consumida el 04/11/2026 a las 13:30',
      );
      expect(html.textContent).toContain('Mostrando del elemento 1 al 2 de 2 totales');
    });

    it('tras cambiar la fecha en el modal avisa y recarga la página', async () => {
      const fixture = await crear();
      const nueva = consumoDePrueba({ fecha: new Date(2026, 10, 3, 9, 0).toISOString() });
      vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
        componentInstance: {},
        result: Promise.resolve({ accion: 'guardado', consumo: nueva }),
      } as never);

      (
        (fixture.nativeElement as HTMLElement).querySelector(
          '[aria-label^="Editar el consumo"]',
        ) as HTMLButtonElement
      ).click();
      await Promise.resolve();

      expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe(
        'Consumo cambiado al 03/11/2026 a las 09:00.',
      );
      servidor.expectOne('/api/consumos?page=0&size=9').flush(paginaDe([nueva]));
    });

    it('sin consumos invita a ver recetas', async () => {
      const fixture = TestBed.createComponent(Historico);
      await fixture.whenStable();
      servidor.expectOne('/api/consumos?page=0&size=9').flush(paginaDe([]));
      await fixture.whenStable();
      expect((fixture.nativeElement as HTMLElement).textContent).toContain(
        'Todavía no has registrado ninguna receta consumida.',
      );
    });
  });

  describe('ModalConsumo', () => {
    async function crear(consumo = consumoDePrueba()) {
      const fixture = TestBed.createComponent(ModalConsumo);
      fixture.componentInstance.consumo = consumo;
      await fixture.whenStable();
      return fixture;
    }

    it('empieza con la fecha del consumo en hora local', async () => {
      const fixture = await crear();
      expect(fixture.componentInstance.formulario.controls.fecha.value).toBe(aValorLocal(FECHA));
      expect((fixture.nativeElement as HTMLElement).textContent).toContain(
        'Consumida el 04/11/2026 a las 13:30',
      );
    });

    it('Guardar envía la nueva fecha en UTC y cierra con el consumo', async () => {
      const fixture = await crear();
      const nueva = new Date(2026, 10, 3, 21, 15);
      fixture.componentInstance.formulario.controls.fecha.setValue(aValorLocal(nueva));

      fixture.componentInstance.guardar();
      const peticion = servidor.expectOne('/api/consumos/15');
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({ fecha: nueva.toISOString() });
      const actualizado = consumoDePrueba({ fecha: nueva.toISOString() });
      peticion.flush(actualizado);

      expect(activo.close).toHaveBeenCalledWith({ accion: 'guardado', consumo: actualizado });
    });

    it('no deja guardar una fecha futura', async () => {
      const fixture = await crear();
      fixture.componentInstance.formulario.controls.fecha.setValue(
        aValorLocal(new Date(Date.now() + 2 * 86_400_000)),
      );
      fixture.componentInstance.guardar();
      servidor.expectNone('/api/consumos/15');
      expect(fixture.componentInstance.formulario.controls.fecha.hasError('futura')).toBe(true);
    });

    it('Eliminar pide confirmación antes de borrar', async () => {
      const fixture = await crear();
      const html = fixture.nativeElement as HTMLElement;
      const botones = () => Array.from(html.querySelectorAll('button'));

      botones()
        .find((b) => b.textContent?.trim() === 'Eliminar')!
        .click();
      await fixture.whenStable();
      servidor.expectNone('/api/consumos/15');
      expect(html.textContent).toContain('¿Seguro que quieres eliminar este consumo?');

      botones()
        .find((b) => b.textContent?.trim() === 'Sí, eliminar')!
        .click();
      const peticion = servidor.expectOne('/api/consumos/15');
      expect(peticion.request.method).toBe('DELETE');
      peticion.flush(null, { status: 204, statusText: 'No Content' });

      expect(activo.close).toHaveBeenCalledWith({
        accion: 'eliminado',
        consumo: consumoDePrueba(),
      });
    });

    it('muestra el error del servidor junto a la fecha', async () => {
      const fixture = await crear();
      fixture.componentInstance.guardar();
      servidor.expectOne('/api/consumos/15').flush(
        {
          codigo: 'VALIDACION',
          mensaje: 'Revisa los datos',
          errores: { fecha: 'La fecha no puede ser futura' },
        },
        { status: 400, statusText: 'Bad Request' },
      );
      await fixture.whenStable();
      expect(
        (fixture.nativeElement as HTMLElement).querySelector('#consumo-nueva-fecha-error')
          ?.textContent,
      ).toContain('La fecha no puede ser futura');
      expect(activo.close).not.toHaveBeenCalled();
    });
  });
});
