import { TestBed } from '@angular/core/testing';
import { loginDePrueba } from '../../testing/jwt';
import { CLAVE_SESION, Sesion } from './sesion';

describe('Sesion: el email confirmado (D-088)', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('se actualiza y se guarda', () => {
    const sesion = TestBed.inject(Sesion);
    const respuesta = loginDePrueba();
    sesion.iniciar({ ...respuesta, usuario: { ...respuesta.usuario, emailVerificado: false } });

    sesion.actualizarEmailVerificado(true);

    expect(sesion.usuario()?.emailVerificado).toBe(true);
    expect(JSON.parse(localStorage.getItem(CLAVE_SESION)!).usuario.emailVerificado).toBe(true);
  });

  it('sin sesión no hace nada', () => {
    const sesion = TestBed.inject(Sesion);
    sesion.actualizarEmailVerificado(true);
    expect(sesion.usuario()).toBeNull();
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });
});

describe('Sesion', () => {
  beforeEach(() => localStorage.clear());

  function crear(): Sesion {
    return TestBed.inject(Sesion);
  }

  it('empieza sin sesión', () => {
    const sesion = crear();
    expect(sesion.autenticado()).toBe(false);
    expect(sesion.usuario()).toBeNull();
    expect(sesion.esAdmin()).toBe(false);
  });

  it('inicia la sesión y la guarda en localStorage', () => {
    const sesion = crear();
    const login = loginDePrueba(['USER', 'ADMIN']);

    sesion.iniciar(login);

    expect(sesion.autenticado()).toBe(true);
    expect(sesion.usuario()?.nombre).toBe('Lucía');
    expect(sesion.esAdmin()).toBe(true);
    expect(JSON.parse(localStorage.getItem(CLAVE_SESION)!).caduca).toBe(login.caduca);
  });

  it('recupera la sesión guardada al arrancar', () => {
    const login = loginDePrueba();
    localStorage.setItem(CLAVE_SESION, JSON.stringify(login));

    const sesion = crear();

    expect(sesion.autenticado()).toBe(true);
    expect(sesion.usuario()?.id).toBe('lucia');
    expect(sesion.esAdmin()).toBe(false);
  });

  it('descarta al arrancar una sesión con el token caducado', () => {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(loginDePrueba(['USER'], -60)));

    const sesion = crear();

    expect(sesion.autenticado()).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('descarta una sesión antigua, con el token en localStorage (D-070)', () => {
    const { usuario } = loginDePrueba();
    localStorage.setItem(CLAVE_SESION, JSON.stringify({ token: 'a.b.c', usuario }));

    const sesion = crear();

    expect(sesion.autenticado()).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('descarta al arrancar una sesión guardada que no es JSON válido', () => {
    localStorage.setItem(CLAVE_SESION, '{roto');
    expect(crear().autenticado()).toBe(false);
  });

  it('cierra la sesión y la borra de localStorage', () => {
    const sesion = crear();
    sesion.iniciar(loginDePrueba());

    sesion.cerrar();

    expect(sesion.autenticado()).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('vigente() cierra la sesión cuando el token caduca', () => {
    const sesion = crear();
    sesion.iniciar(loginDePrueba(['USER'], 60));

    expect(sesion.vigente()).toBe(true);
    expect(sesion.vigente(Date.now() + 61_000)).toBe(false);
    expect(sesion.autenticado()).toBe(false);
  });
});
