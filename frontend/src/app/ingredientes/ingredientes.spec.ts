import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable } from 'rxjs';
import { loginDePrueba } from '../../testing/jwt';
import { GRUPO_CARNE, GRUPO_HORTALIZAS, ingredienteDePrueba, paginaDe } from '../../testing/datos';
import { textoRaciones } from '../core/catalogo-api';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { ListaIngredientes } from './lista-ingredientes';
import { ModalIngrediente } from './modal-ingrediente';

describe('ingredientes', () => {
  let servidor: HttpTestingController;
  let activo: { close: ReturnType<typeof vi.fn>; dismiss: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    activo = { close: vi.fn(), dismiss: vi.fn() };
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

  it('describe las raciones del grupo', () => {
    expect(textoRaciones(GRUPO_CARNE)).toBe('3 raciones/semana');
    expect(textoRaciones({ ...GRUPO_CARNE, racionesSemana: 1 })).toBe('1 ración/semana');
    expect(textoRaciones(GRUPO_HORTALIZAS)).toBe('sin límite');
  });

  describe('ListaIngredientes', () => {
    async function crear() {
      const fixture = TestBed.createComponent(ListaIngredientes);
      await fixture.whenStable();
      servidor.expectOne('/api/grupos').flush([GRUPO_CARNE, GRUPO_HORTALIZAS]);
      servidor.expectOne('/api/ingredientes?page=0&size=16').flush(
        paginaDe(
          [
            ingredienteDePrueba(),
            ingredienteDePrueba({
              id: 30,
              nombre: 'Ternera',
              grupo: GRUPO_CARNE,
              alimento: null,
            }),
          ],
          { tamano: 16 },
        ),
      );
      await fixture.whenStable();
      return fixture;
    }

    it('muestra nombre, grupo y raciones de cada ingrediente', async () => {
      const html = (await crear()).nativeElement as HTMLElement;
      const tarjetas = Array.from(html.querySelectorAll('.tarjeta-ingrediente'));
      expect(tarjetas.length).toBe(2);
      expect(tarjetas[1].textContent).toContain('Ternera');
      expect(tarjetas[1].textContent).toContain('Carne');
      expect(tarjetas[1].textContent).toContain('(3 raciones/semana)');
      expect(tarjetas[0].textContent).toContain('(sin límite)');
      expect(html.textContent).toContain('Mostrando del elemento 1 al 2 de 2 totales');
    });

    it('un usuario sin rol ADMIN no ve el filtro de datos nutricionales', async () => {
      const html = (await crear()).nativeElement as HTMLElement;
      expect(html.querySelector('#ingredientes-sin-alimento')).toBeNull();
      expect(html.querySelectorAll('.alimento')[1].textContent).toContain(
        'Sin datos nutricionales',
      );
    });

    it('un usuario sin rol ADMIN no ve el botón de editar', async () => {
      const html = (await crear()).nativeElement as HTMLElement;
      expect(html.querySelector('[aria-label="Editar Patata"]')).toBeNull();
    });

    it('el administrador ve el botón de editar en cada ingrediente', async () => {
      TestBed.inject(Sesion).iniciar(loginDePrueba(['USER', 'ADMIN']));
      const html = (await crear()).nativeElement as HTMLElement;
      expect(html.querySelector('[aria-label="Editar Patata"]')).toBeTruthy();
      expect(html.querySelector('[aria-label="Editar Ternera"]')).toBeTruthy();
    });

    it('filtra por nombre y grupo, y Restablecer los quita', async () => {
      const fixture = await crear();
      const componente = fixture.componentInstance;
      // «Sin datos nutricionales» solo cuenta para los administradores (D-080)
      componente.formulario.setValue({ nombre: ' pat ', grupo: 4, sinAlimento: true });

      componente.filtrar();
      servidor.expectOne('/api/ingredientes?page=0&size=16&nombre=pat&grupo=4').flush(paginaDe([]));

      componente.restablecer();
      servidor.expectOne('/api/ingredientes?page=0&size=16').flush(paginaDe([]));
      expect(componente.formulario.getRawValue()).toEqual({
        nombre: '',
        grupo: null,
        sinAlimento: false,
      });
    });

    it('al editar en el modal sustituye el ingrediente en la lista', async () => {
      TestBed.inject(Sesion).iniciar(loginDePrueba(['USER', 'ADMIN']));
      const fixture = await crear();
      const modales = TestBed.inject(NgbModal);
      const editado = ingredienteDePrueba({ nombre: 'Patata nueva' });
      vi.spyOn(modales, 'open').mockReturnValue({
        componentInstance: {},
        result: Promise.resolve(editado),
      } as never);

      fixture.componentInstance.editar(ingredienteDePrueba());
      await Promise.resolve();
      await fixture.whenStable();

      expect((fixture.nativeElement as HTMLElement).textContent).toContain('Patata nueva');
    });
  });

  describe('ListaIngredientes (administración, D-065)', () => {
    async function crearComoAdmin() {
      TestBed.inject(Sesion).iniciar(loginDePrueba(['USER', 'ADMIN']));
      const fixture = TestBed.createComponent(ListaIngredientes);
      await fixture.whenStable();
      servidor.expectOne('/api/grupos').flush([GRUPO_CARNE, GRUPO_HORTALIZAS]);
      servidor
        .expectOne('/api/ingredientes?page=0&size=16')
        .flush(paginaDe([ingredienteDePrueba()], { tamano: 16 }));
      await fixture.whenStable();
      return fixture;
    }

    it('cada ingrediente dice su alimento de BEDCA o que no tiene datos (D-080)', async () => {
      const html = (await crearComoAdmin()).nativeElement as HTMLElement;
      expect(html.querySelector('.alimento')!.textContent!.replace(/\s+/g, ' ').trim()).toBe(
        'Datos nutricionales de BEDCA: Patata, cruda',
      );
      expect(html.querySelector('[aria-label="Datos nutricionales de Patata"]')).toBeTruthy();
    });

    it('el filtro «sin datos nutricionales» va al servidor solo si se marca', async () => {
      const fixture = await crearComoAdmin();
      const html = fixture.nativeElement as HTMLElement;
      html.querySelector<HTMLInputElement>('#ingredientes-sin-alimento')!.click();

      fixture.componentInstance.filtrar();
      servidor
        .expectOne('/api/ingredientes?page=0&size=16&sinAlimento=true')
        .flush(paginaDe([ingredienteDePrueba({ alimento: null })], { tamano: 16 }));
      await fixture.whenStable();
      expect(html.querySelector('.alimento')!.textContent).toContain('Sin datos nutricionales');

      fixture.componentInstance.restablecer();
      servidor.expectOne('/api/ingredientes?page=0&size=16').flush(paginaDe([]));
      expect(fixture.componentInstance.formulario.controls.sinAlimento.value).toBe(false);
    });

    it('al asignar un alimento lo cambia en la lista; con el filtro, recarga', async () => {
      const fixture = await crearComoAdmin();
      const open = vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
        componentInstance: {},
        result: Promise.resolve(ingredienteDePrueba({ alimento: 'Patata, cocida' })),
      } as never);

      fixture.componentInstance.datosNutricionales(ingredienteDePrueba());
      await Promise.resolve();
      await fixture.whenStable();

      const avisos = TestBed.inject(Notificaciones).avisos();
      expect(avisos[0].texto).toBe(
        'Datos nutricionales de «Patata» guardados: usa «Patata, cocida».',
      );
      expect((fixture.nativeElement as HTMLElement).textContent).toContain('Patata, cocida');

      fixture.componentInstance.formulario.controls.sinAlimento.setValue(true);
      fixture.componentInstance.filtrar();
      servidor
        .expectOne('/api/ingredientes?page=0&size=16&sinAlimento=true')
        .flush(paginaDe([ingredienteDePrueba({ alimento: null })], { tamano: 16 }));
      open.mockReturnValue({
        componentInstance: {},
        result: Promise.resolve(ingredienteDePrueba({ alimento: null })),
      } as never);
      fixture.componentInstance.datosNutricionales(ingredienteDePrueba({ alimento: null }));
      await Promise.resolve();
      expect(TestBed.inject(Notificaciones).avisos().at(-1)!.texto).toBe(
        'Datos nutricionales de «Patata» guardados: no tiene alimento.',
      );
      servidor.expectOne('/api/ingredientes?page=0&size=16&sinAlimento=true').flush(paginaDe([]));
    });

    it('el administrador puede fusionar y borrar cada ingrediente', async () => {
      const html = (await crearComoAdmin()).nativeElement as HTMLElement;
      expect(html.querySelector('[aria-label="Fusionar Patata con otro"]')).toBeTruthy();
      expect(html.querySelector('[aria-label="Borrar Patata"]')).toBeTruthy();
    });

    it('tras fusionar avisa de cuántas recetas cambian y recarga', async () => {
      const fixture = await crearComoAdmin();
      vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
        componentInstance: {},
        result: Promise.resolve({
          ingrediente: ingredienteDePrueba({ id: 27, nombre: 'Patata nueva' }),
          recetas: 3,
        }),
      } as never);

      fixture.componentInstance.fusionar(ingredienteDePrueba());
      await Promise.resolve();

      expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe(
        '«Patata» se ha fusionado con «Patata nueva»: 3 recetas actualizadas.',
      );
      servidor.expectOne('/api/ingredientes?page=0&size=16').flush(paginaDe([]));
    });

    it('borrar pide confirmación y lo hace dentro del modal', async () => {
      const fixture = await crearComoAdmin();
      const open = vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
        componentInstance: {},
        result: new Promise(() => undefined),
      } as never);

      fixture.componentInstance.borrar(ingredienteDePrueba());

      expect(open).toHaveBeenCalled();
      const opciones = (
        open.mock.results[0].value as {
          componentInstance: { opciones: { accion: () => Observable<void> } };
        }
      ).componentInstance.opciones;
      opciones.accion().subscribe();
      const peticion = servidor.expectOne('/api/admin/ingredientes/26');
      expect(peticion.request.method).toBe('DELETE');
      peticion.flush(null, { status: 204, statusText: 'No Content' });
    });
  });

  describe('ModalIngrediente', () => {
    async function crear(ingrediente = null as ReturnType<typeof ingredienteDePrueba> | null) {
      const fixture = TestBed.createComponent(ModalIngrediente);
      fixture.componentInstance.ingrediente = ingrediente;
      fixture.componentInstance.nombreInicial = 'Quin';
      await fixture.whenStable();
      servidor.expectOne('/api/grupos').flush([GRUPO_CARNE, GRUPO_HORTALIZAS]);
      await fixture.whenStable();
      return fixture;
    }

    it('crea el ingrediente y cierra el modal con él', async () => {
      const fixture = await crear();
      const componente = fixture.componentInstance;
      expect(componente.formulario.controls.nombre.value).toBe('Quin');
      componente.formulario.setValue({ nombre: 'Quinoa ', grupo: 4 });

      componente.guardar();
      const peticion = servidor.expectOne('/api/ingredientes');
      expect(peticion.request.body).toEqual({ nombre: 'Quinoa', grupo: 4 });
      const creado = ingredienteDePrueba({ id: 80, nombre: 'Quinoa' });
      peticion.flush(creado);

      expect(activo.close).toHaveBeenCalledWith(creado);
    });

    it('si el nombre ya existe, lo indica junto al campo', async () => {
      const fixture = await crear();
      const componente = fixture.componentInstance;
      componente.formulario.setValue({ nombre: 'Patata', grupo: 4 });
      componente.guardar();
      servidor.expectOne('/api/ingredientes').flush(
        {
          codigo: 'CONFLICTO',
          mensaje: 'Ya existe',
          errores: { nombre: 'Ya existe ese ingrediente' },
        },
        { status: 409, statusText: 'Conflict' },
      );
      await fixture.whenStable();

      expect(
        (fixture.nativeElement as HTMLElement).querySelector('#ingrediente-nombre-error')
          ?.textContent,
      ).toContain('Ya existe ese ingrediente');
      expect(activo.close).not.toHaveBeenCalled();
    });

    it('sin el email confirmado no deja crear y dice por qué (D-089)', async () => {
      const login = loginDePrueba();
      TestBed.inject(Sesion).iniciar({
        ...login,
        usuario: { ...login.usuario, emailVerificado: false },
      });
      const fixture = await crear();
      const componente = fixture.componentInstance;
      const html = fixture.nativeElement as HTMLElement;
      componente.formulario.setValue({ nombre: 'Quinoa', grupo: 4 });
      await fixture.whenStable();

      expect(html.textContent).toContain('confirma antes tu email');
      expect((html.querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBe(true);
      componente.guardar();
      servidor.expectNone('/api/ingredientes');
    });

    it('un administrador lo crea aunque no haya confirmado su email', async () => {
      const login = loginDePrueba(['USER', 'ADMIN']);
      TestBed.inject(Sesion).iniciar({
        ...login,
        usuario: { ...login.usuario, emailVerificado: false },
      });
      const fixture = await crear();
      fixture.componentInstance.formulario.setValue({ nombre: 'Quinoa', grupo: 4 });
      await fixture.whenStable();

      expect((fixture.nativeElement as HTMLElement).textContent).not.toContain(
        'confirma antes tu email',
      );
      fixture.componentInstance.guardar();
      servidor.expectOne('/api/ingredientes').flush(ingredienteDePrueba({ id: 80 }));
      expect(activo.close).toHaveBeenCalled();
    });

    it('en edición rellena los datos y usa PUT', async () => {
      const fixture = await crear(ingredienteDePrueba());
      const componente = fixture.componentInstance;
      expect(componente.formulario.getRawValue()).toEqual({ nombre: 'Patata', grupo: 4 });
      expect((fixture.nativeElement as HTMLElement).textContent).toContain('Editar ingrediente');

      componente.guardar();
      const peticion = servidor.expectOne('/api/ingredientes/26');
      expect(peticion.request.method).toBe('PUT');
      peticion.flush(ingredienteDePrueba());
      expect(activo.close).toHaveBeenCalled();
    });
  });
});
