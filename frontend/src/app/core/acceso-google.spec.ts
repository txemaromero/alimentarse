import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { AccesoGoogle, URL_ACCESO_GOOGLE } from './acceso-google';

describe('AccesoGoogle', () => {
  const assign = vi.fn();
  let acceso: AccesoGoogle;

  beforeEach(() => {
    sessionStorage.clear();
    assign.mockReset();
    TestBed.configureTestingModule({
      providers: [{ provide: DOCUMENT, useValue: { location: { assign } } }],
    });
    acceso = TestBed.inject(AccesoGoogle);
  });

  it('va al backend, que lleva a Google, y recuerda adónde volver una sola vez', () => {
    acceso.entrar('/recetas/3');

    expect(assign).toHaveBeenCalledWith(URL_ACCESO_GOOGLE);
    expect(acceso.destino()).toBe('/recetas/3');
    expect(acceso.destino()).toBeUndefined();
  });

  it('sin página a la que volver olvida la de un intento anterior', () => {
    acceso.entrar('/recetas/3');
    acceso.entrar();

    expect(acceso.destino()).toBeUndefined();
  });

  it('sin sessionStorage va igualmente', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('bloqueado');
    });

    acceso.entrar('/recetas/3');

    expect(assign).toHaveBeenCalledWith(URL_ACCESO_GOOGLE);
    expect(acceso.destino()).toBeUndefined();
    vi.restoreAllMocks();
  });
});
