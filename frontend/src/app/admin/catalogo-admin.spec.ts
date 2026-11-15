import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { GRUPO_CARNE, GRUPO_HORTALIZAS, ingredienteDePrueba } from '../../testing/datos';
import { CatalogoApi } from '../core/catalogo-api';
import { GrupoAdmin } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { ModalFusionar } from '../ingredientes/modal-fusionar';
import { GruposAlimentos } from './grupos-alimentos';
import { ModalGrupo } from './modal-grupo';

const CARNE: GrupoAdmin = { ...GRUPO_CARNE, basico: false, ingredientes: 12 };
const VACIO: GrupoAdmin = {
  id: 20,
  nombre: 'Marisco',
  racionesSemana: null,
  basico: false,
  ingredientes: 0,
};

describe('Catálogo en la administración', () => {
  let servidor: HttpTestingController;
  const activo = { close: vi.fn(), dismiss: vi.fn() };

  beforeEach(() => {
    activo.close.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NgbActiveModal, useValue: activo },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  describe('CatalogoApi', () => {
    it('guarda los grupos hasta que se invalidan', () => {
      const catalogo = TestBed.inject(CatalogoApi);
      catalogo.grupos$.subscribe();
      catalogo.grupos$.subscribe();
      servidor.expectOne('/api/grupos').flush([GRUPO_CARNE]);

      catalogo.invalidarGrupos();
      catalogo.grupos$.subscribe();
      servidor.expectOne('/api/grupos').flush([GRUPO_CARNE, GRUPO_HORTALIZAS]);
    });
  });

  describe('GruposAlimentos', () => {
    async function crear() {
      const fixture = TestBed.createComponent(GruposAlimentos);
      await fixture.whenStable();
      servidor.expectOne('/api/admin/grupos').flush([CARNE, VACIO]);
      await fixture.whenStable();
      return fixture;
    }

    it('muestra las raciones, si es básico y cuántos ingredientes tiene', async () => {
      const html = (await crear()).nativeElement as HTMLElement;
      const filas = html.querySelectorAll('tbody tr');
      expect(filas.length).toBe(2);
      expect(filas[0].textContent).toContain('3 raciones/semana');
      expect(filas[0].textContent).toContain('12');
      expect(filas[1].textContent).toContain('Sin límite');
    });

    it('no deja borrar un grupo con ingredientes', async () => {
      const html = (await crear()).nativeElement as HTMLElement;
      const borrarCarne = html.querySelector(
        'tbody [aria-label="Borrar Carne"]',
      ) as HTMLButtonElement;
      const borrarMarisco = html.querySelector(
        'tbody [aria-label="Borrar Marisco"]',
      ) as HTMLButtonElement;
      expect(borrarCarne.disabled).toBe(true);
      expect(borrarCarne.title).toContain('no se puede borrar');
      expect(borrarMarisco.disabled).toBe(false);
    });

    it('tras editar un grupo recarga la lista e invalida la caché de grupos', async () => {
      const fixture = await crear();
      const invalidar = vi.spyOn(TestBed.inject(CatalogoApi), 'invalidarGrupos');
      vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
        componentInstance: {},
        result: Promise.resolve({ ...CARNE, racionesSemana: 2 }),
      } as never);

      fixture.componentInstance.editar(CARNE);
      await Promise.resolve();

      expect(invalidar).toHaveBeenCalled();
      expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe('Grupo «Carne» guardado.');
      servidor.expectOne('/api/admin/grupos').flush([{ ...CARNE, racionesSemana: 2 }, VACIO]);
    });
  });

  describe('ModalGrupo', () => {
    async function crear(grupo?: GrupoAdmin) {
      const fixture = TestBed.createComponent(ModalGrupo);
      fixture.componentInstance.grupo = grupo;
      await fixture.whenStable();
      return fixture;
    }

    it('crea un grupo sin límite sin pedir las raciones', async () => {
      const fixture = await crear();
      const formulario = fixture.componentInstance.formulario;
      formulario.patchValue({ nombre: ' Marisco ', sinLimite: true, basico: false });
      expect(formulario.valid).toBe(true);

      fixture.componentInstance.guardar();
      const peticion = servidor.expectOne('/api/admin/grupos');
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({
        nombre: 'Marisco',
        racionesSemana: null,
        basico: false,
      });
      peticion.flush(VACIO);

      expect(activo.close).toHaveBeenCalledWith(VACIO);
    });

    it('con límite, las raciones son obligatorias y al menos 1', async () => {
      const fixture = await crear();
      const formulario = fixture.componentInstance.formulario;
      formulario.patchValue({ nombre: 'Marisco', racionesSemana: 0 });
      expect(formulario.controls.racionesSemana.invalid).toBe(true);
      formulario.patchValue({ racionesSemana: null });
      expect(formulario.controls.racionesSemana.hasError('required')).toBe(true);
    });

    it('al editar rellena los datos y usa PUT', async () => {
      const fixture = await crear(CARNE);
      const formulario = fixture.componentInstance.formulario;
      expect(formulario.getRawValue()).toEqual({
        nombre: 'Carne',
        sinLimite: false,
        racionesSemana: 3,
        basico: false,
      });
      formulario.patchValue({ racionesSemana: 2 });

      fixture.componentInstance.guardar();
      const peticion = servidor.expectOne('/api/admin/grupos/1');
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body).toEqual({ nombre: 'Carne', racionesSemana: 2, basico: false });
      peticion.flush({ ...CARNE, racionesSemana: 2 });
    });

    it('un nombre repetido se marca en el campo', async () => {
      const fixture = await crear();
      fixture.componentInstance.formulario.patchValue({ nombre: 'Carne', racionesSemana: 2 });
      fixture.componentInstance.guardar();
      servidor.expectOne('/api/admin/grupos').flush(
        {
          codigo: 'CONFLICTO',
          mensaje: 'Ya existe un grupo con ese nombre.',
          errores: { nombre: 'Ya existe un grupo con ese nombre' },
        },
        { status: 409, statusText: 'Conflict' },
      );
      expect(fixture.componentInstance.formulario.controls.nombre.errors).toEqual({
        servidor: 'Ya existe un grupo con ese nombre',
      });
    });
  });

  describe('ModalFusionar', () => {
    async function crear() {
      const fixture = TestBed.createComponent(ModalFusionar);
      fixture.componentInstance.origen = ingredienteDePrueba({ id: 90, nombre: 'Patata nueva' });
      await fixture.whenStable();
      return fixture;
    }

    it('exige elegir una sugerencia, no basta con escribir', async () => {
      const fixture = await crear();
      fixture.componentInstance.formulario.controls.destino.setValue('Patata');
      expect(fixture.componentInstance.formulario.invalid).toBe(true);
    });

    it('avisa si el elegido es de otro grupo', async () => {
      const fixture = await crear();
      fixture.componentInstance.formulario.controls.destino.setValue(
        ingredienteDePrueba({ id: 30, nombre: 'Ternera', grupo: GRUPO_CARNE }),
      );
      await fixture.whenStable();
      expect(
        (fixture.nativeElement as HTMLElement).querySelector('.alert-warning')?.textContent,
      ).toContain('pasarán a contar como Carne');
    });

    it('fusiona con el elegido y cierra con el resultado', async () => {
      const fixture = await crear();
      const patata = ingredienteDePrueba();
      fixture.componentInstance.formulario.controls.destino.setValue(patata);

      fixture.componentInstance.fusionar();
      const peticion = servidor.expectOne('/api/admin/ingredientes/90/fusionar');
      expect(peticion.request.body).toEqual({ destino: 26 });
      peticion.flush({ ingrediente: patata, recetas: 3 });

      expect(activo.close).toHaveBeenCalledWith({ ingrediente: patata, recetas: 3 });
    });
  });
});
