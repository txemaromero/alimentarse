import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject, of, throwError } from 'rxjs';
import { ingredienteDePrueba } from '../../testing/datos';
import { AdminApi } from '../admin/admin-api';
import { AlimentoBedca, NutricionIngrediente } from '../core/modelos';
import {
  MAXIMO_PIEZAS,
  ModalDatosNutricionales,
  busquedaAlimentos,
  nombreAlimento,
} from './modal-datos-nutricionales';

registerLocaleData(localeEs);

const POLLO: AlimentoBedca = {
  id: 2292,
  nombre: 'Pollo, entero, con piel, crudo',
  energiaKcal: 167,
  energiaKj: 696.7,
  grasas: 9.25,
  grasasSaturadas: 2.66,
  hidratos: 0,
  fibra: 0,
  proteinas: 20.85,
  sodioMg: 63,
};

describe('busquedaAlimentos', () => {
  let alimentosBedca: ReturnType<typeof vi.fn<(q: string) => Observable<AlimentoBedca[]>>>;
  let texto$: Subject<string>;
  let recibidas: AlimentoBedca[][];

  beforeEach(() => {
    vi.useFakeTimers();
    alimentosBedca = vi.fn(() => of([POLLO]));
    texto$ = new Subject<string>();
    recibidas = [];
    busquedaAlimentos({ alimentosBedca } as unknown as AdminApi)(texto$).subscribe((lista) =>
      recibidas.push(lista),
    );
  });

  afterEach(() => vi.useRealTimers());

  it('busca a partir de dos caracteres, cuando se deja de escribir', () => {
    texto$.next('p');
    vi.advanceTimersByTime(250);
    expect(alimentosBedca).not.toHaveBeenCalled();
    expect(recibidas).toEqual([[]]);

    texto$.next(' pollo crudo ');
    vi.advanceTimersByTime(250);
    expect(alimentosBedca).toHaveBeenCalledExactlyOnceWith('pollo crudo');
    expect(recibidas.at(-1)).toEqual([POLLO]);
  });

  it('un error es una lista vacía', () => {
    alimentosBedca.mockReturnValue(throwError(() => new Error('red')));
    texto$.next('pollo');
    vi.advanceTimersByTime(250);
    expect(recibidas).toEqual([[]]);
  });

  it('el nombre del alimento o el texto escrito', () => {
    expect(nombreAlimento(POLLO)).toBe('Pollo, entero, con piel, crudo');
    expect(nombreAlimento('pol')).toBe('pol');
    expect(nombreAlimento(null)).toBe('');
  });
});

describe('ModalDatosNutricionales', () => {
  let servidor: HttpTestingController;
  let activo: { close: ReturnType<typeof vi.fn>; dismiss: ReturnType<typeof vi.fn> };
  let fixture: ComponentFixture<ModalDatosNutricionales>;
  let html: HTMLElement;

  beforeEach(() => {
    activo = { close: vi.fn(), dismiss: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NgbActiveModal, useValue: activo },
        { provide: LOCALE_ID, useValue: 'es' },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
  });

  afterEach(() => servidor.verify());

  async function crear(
    datos: NutricionIngrediente | null,
    id = 26,
  ): Promise<ModalDatosNutricionales> {
    fixture = TestBed.createComponent(ModalDatosNutricionales);
    fixture.componentInstance.ingrediente = ingredienteDePrueba({ id });
    html = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
    const peticion = servidor.expectOne(`/api/admin/ingredientes/${id}/nutricion`);
    if (datos) {
      peticion.flush(datos);
    } else {
      peticion.flush(null, { status: 500, statusText: 'Error' });
    }
    await fixture.whenStable();
    return fixture.componentInstance;
  }

  function texto(elemento: Element | null): string {
    return (elemento?.textContent ?? '').replace(/\s+/g, ' ').trim();
  }

  it('carga el alimento con sus valores y las piezas', async () => {
    await crear({ alimento: POLLO, piezas: [{ pieza: 'unidad', gramos: 1500 }] });

    expect(texto(html.querySelector('h2'))).toBe('Datos nutricionales de «Patata»');
    expect(html.querySelector<HTMLInputElement>('#alimento-bedca')!.value).toBe(POLLO.nombre);
    expect(texto(html.querySelector('caption'))).toBe(
      'Por 100 g de «Pollo, entero, con piel, crudo»',
    );
    expect(texto(html.querySelector('.composicion tr'))).toBe('Energía 167 kcal · 696,7 kJ');
    expect(html.querySelector<HTMLInputElement>('#pieza-0')!.value).toBe('unidad');
    expect(html.querySelector<HTMLInputElement>('#pieza-0-gramos')!.value).toBe('1500');
    expect(html.textContent).toContain('Fuente: AESAN/BEDCA');
    expect(document.activeElement?.id).toBe('alimento-bedca');
  });

  it('guarda el alimento elegido y las piezas, sin espacios', async () => {
    const modal = await crear({ alimento: null, piezas: [] });
    expect(html.querySelector('.composicion')).toBeNull();

    modal.formulario.controls.alimento.setValue({ ...POLLO, grasasSaturadas: null });
    modal.anadirPieza();
    modal.piezas.at(0).setValue({ pieza: ' Unidad ', gramos: 1500 });
    await fixture.whenStable();
    expect(texto(html.querySelectorAll('.composicion tr')[2])).toBe(
      'de las cuales saturadas sin dato',
    );

    modal.guardar();
    const peticion = servidor.expectOne('/api/admin/ingredientes/26/nutricion');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      alimento: 2292,
      piezas: [{ pieza: 'Unidad', gramos: 1500 }],
    });
    const actualizado = ingredienteDePrueba({ alimento: POLLO.nombre });
    peticion.flush(actualizado);
    expect(activo.close).toHaveBeenCalledWith(actualizado);
  });

  it('quitar el alimento y las piezas guarda null y una lista vacía', async () => {
    const modal = await crear({ alimento: POLLO, piezas: [{ pieza: 'lata', gramos: 60 }] });

    html.querySelector<HTMLButtonElement>('.input-group .btn-outline-secondary')!.click();
    html.querySelector<HTMLButtonElement>('[aria-label="Quitar la pieza 1"]')!.click();
    await fixture.whenStable();
    expect(html.querySelector('.composicion')).toBeNull();

    modal.guardar();
    const peticion = servidor.expectOne('/api/admin/ingredientes/26/nutricion');
    expect(peticion.request.body).toEqual({ alimento: null, piezas: [] });
    peticion.flush(ingredienteDePrueba({ alimento: null }));
  });

  it('un texto sin elegir o una pieza mal escrita no se pueden guardar', async () => {
    const modal = await crear({ alimento: null, piezas: [] });

    modal.formulario.controls.alimento.setValue('pollo');
    modal.anadirPieza();
    modal.piezas.at(0).setValue({ pieza: '2 latas', gramos: 0 });
    modal.guardar();
    await fixture.whenStable();

    expect(texto(html.querySelector('#alimento-bedca-error'))).toBe(
      'Elige una de las sugerencias, o deja el campo vacío',
    );
    expect(texto(html.querySelector('#pieza-0-error'))).toBe('Una sola palabra, sin números');
    expect(texto(html.querySelector('#pieza-0-gramos-error'))).toBe('El valor mínimo es 0,1');
  });

  it('no deja añadir más piezas de las que admite el servidor', async () => {
    const modal = await crear({ alimento: null, piezas: [] });

    for (let i = 0; i < MAXIMO_PIEZAS + 2; i++) {
      modal.anadirPieza();
    }
    await fixture.whenStable();
    expect(modal.piezas.length).toBe(MAXIMO_PIEZAS);
    const anadir = [...html.querySelectorAll<HTMLButtonElement>('.piezas > button')].at(-1)!;
    expect(anadir.disabled).toBe(true);
  });

  it('los errores del servidor van a su campo, también en las piezas', async () => {
    const modal = await crear({ alimento: POLLO, piezas: [{ pieza: 'lata', gramos: 60 }] });

    modal.guardar();
    servidor.expectOne('/api/admin/ingredientes/26/nutricion').flush(
      {
        codigo: 'VALIDACION',
        mensaje: 'Revisa',
        errores: { 'piezas[0].pieza': 'Esta unidad ya se convierte sola' },
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    expect(texto(html.querySelector('#pieza-0-error'))).toBe('Esta unidad ya se convierte sola');
    // Hasta que se cambia el campo, no se puede volver a enviar
    modal.guardar();
    servidor.expectNone('/api/admin/ingredientes/26/nutricion');

    modal.piezas.at(0).controls.pieza.setValue('lata');
    modal.guardar();
    servidor
      .expectOne('/api/admin/ingredientes/26/nutricion')
      .flush(
        { codigo: 'NO_ENCONTRADO', mensaje: 'El ingrediente no existe.', errores: {} },
        { status: 404, statusText: 'Not Found' },
      );
    await fixture.whenStable();
    expect(texto(html.querySelector('.alert-danger'))).toBe('El ingrediente no existe.');
    expect(activo.close).not.toHaveBeenCalled();
  });

  it('si no carga, se puede reintentar', async () => {
    const modal = await crear(null);

    expect(texto(html.querySelector('.alert-danger'))).toContain(
      'No se han podido cargar los datos nutricionales.',
    );
    // Sin los datos cargados no se guarda: borraría los que tiene
    modal.guardar();
    servidor.expectNone('/api/admin/ingredientes/26/nutricion');
    html.querySelector<HTMLButtonElement>('.alert-danger button')!.click();
    servidor
      .expectOne('/api/admin/ingredientes/26/nutricion')
      .flush({ alimento: null, piezas: [] });
    await fixture.whenStable();
    expect(html.querySelector('#alimento-bedca')).not.toBeNull();
  });
});
