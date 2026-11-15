import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { detalleDePrueba, paginaDe, recetaDePrueba } from '../../testing/datos';
import { ModalOcultarReceta } from './modal-ocultar-receta';
import { RecetasOcultas } from './recetas-ocultas';

describe('Moderación', () => {
  let servidor: HttpTestingController;
  const activo = { close: vi.fn(), dismiss: vi.fn() };

  beforeEach(() => {
    activo.close.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NgbActiveModal, useValue: activo },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  describe('ModalOcultarReceta', () => {
    async function crear() {
      const fixture = TestBed.createComponent(ModalOcultarReceta);
      fixture.componentInstance.receta = detalleDePrueba();
      await fixture.whenStable();
      return fixture;
    }

    it('el motivo es obligatorio y no vale solo con espacios', async () => {
      const fixture = await crear();
      const motivo = fixture.componentInstance.formulario.controls.motivo;
      motivo.setValue('   ');
      fixture.componentInstance.ocultar();

      expect(motivo.invalid).toBe(true);
      servidor.expectNone('/api/admin/recetas/1/oculta');
    });

    it('envía el motivo sin espacios y cierra con la receta oculta', async () => {
      const fixture = await crear();
      fixture.componentInstance.formulario.controls.motivo.setValue('  Spam  ');
      fixture.componentInstance.ocultar();

      const peticion = servidor.expectOne('/api/admin/recetas/1/oculta');
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({ oculta: true, motivo: 'Spam' });
      const oculta = detalleDePrueba({ oculta: true, motivoOculta: 'Spam' });
      peticion.flush(oculta);

      expect(activo.close).toHaveBeenCalledWith(oculta);
    });

    it('un error sin campos se muestra en el modal', async () => {
      const fixture = await crear();
      fixture.componentInstance.formulario.controls.motivo.setValue('Spam');
      fixture.componentInstance.ocultar();
      servidor
        .expectOne('/api/admin/recetas/1/oculta')
        .flush(
          { codigo: 'NO_ENCONTRADO', mensaje: 'La receta no existe.', errores: {} },
          { status: 404, statusText: 'Not Found' },
        );
      await fixture.whenStable();

      expect((fixture.nativeElement as HTMLElement).textContent).toContain('La receta no existe.');
      expect(activo.close).not.toHaveBeenCalled();
    });
  });

  describe('RecetasOcultas', () => {
    it('lista las recetas ocultas', async () => {
      const fixture = TestBed.createComponent(RecetasOcultas);
      await fixture.whenStable();
      servidor
        .expectOne('/api/admin/recetas/ocultas?page=0&size=9')
        .flush(paginaDe([recetaDePrueba({ oculta: true })]));
      await fixture.whenStable();

      const html = fixture.nativeElement as HTMLElement;
      expect(html.querySelectorAll('app-tarjeta-receta').length).toBe(1);
      expect(html.textContent).toContain('Oculta');
    });

    it('sin ninguna, lo dice', async () => {
      const fixture = TestBed.createComponent(RecetasOcultas);
      await fixture.whenStable();
      servidor.expectOne('/api/admin/recetas/ocultas?page=0&size=9').flush(paginaDe([]));
      await fixture.whenStable();

      expect((fixture.nativeElement as HTMLElement).textContent).toContain(
        'No hay ninguna receta oculta.',
      );
    });
  });
});
