import { TestBed } from '@angular/core/testing';
import { CLAVE_MARCAS_COMPRA } from '../core/datos-sin-conexion';
import { DIAS_MARCA, MarcasCompra } from './marcas-compra';

describe('MarcasCompra', () => {
  const DIA = 24 * 60 * 60 * 1000;
  const AHORA = Date.UTC(2026, 10, 13, 18);
  let marcas: MarcasCompra;

  beforeEach(() => {
    localStorage.removeItem(CLAVE_MARCAS_COMPRA);
    marcas = TestBed.inject(MarcasCompra);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.removeItem(CLAVE_MARCAS_COMPRA);
  });

  it('recuerda lo marcado y lo olvida al desmarcarlo', () => {
    marcas.marcar(5, true, AHORA);
    marcas.marcar(9, true, AHORA);
    expect(marcas.leer(AHORA)).toEqual(new Set([5, 9]));

    marcas.marcar(5, false, AHORA);
    expect(marcas.leer(AHORA)).toEqual(new Set([9]));
  });

  it(`una marca caduca a los ${DIAS_MARCA} días`, () => {
    marcas.marcar(5, true, AHORA);
    expect(marcas.leer(AHORA + (DIAS_MARCA * DIA - 1))).toEqual(new Set([5]));
    expect(marcas.leer(AHORA + DIAS_MARCA * DIA)).toEqual(new Set());
  });

  it('desmarca varios a la vez y no deja nada guardado si no queda ninguno', () => {
    marcas.marcar(5, true, AHORA);
    marcas.marcar(9, true, AHORA);
    marcas.desmarcar([5, 9], AHORA);
    expect(marcas.leer(AHORA)).toEqual(new Set());
    expect(localStorage.getItem(CLAVE_MARCAS_COMPRA)).toBeNull();
  });

  it('ignora lo guardado que no entiende', () => {
    localStorage.setItem(CLAVE_MARCAS_COMPRA, 'no es JSON');
    expect(marcas.leer(AHORA)).toEqual(new Set());

    localStorage.setItem(CLAVE_MARCAS_COMPRA, '[1, 2]');
    expect(marcas.leer(AHORA)).toEqual(new Set());

    localStorage.setItem(CLAVE_MARCAS_COMPRA, JSON.stringify({ 5: 'ayer', x: AHORA, 9: AHORA }));
    expect(marcas.leer(AHORA)).toEqual(new Set([9]));
  });

  it('sin almacenamiento local no falla', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('Sin acceso');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('Cuota llena');
    });
    expect(() => marcas.marcar(5, true, AHORA)).not.toThrow();
    expect(marcas.leer(AHORA)).toEqual(new Set());
  });
});
