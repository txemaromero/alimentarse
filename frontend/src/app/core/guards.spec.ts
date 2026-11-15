import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { adminGuard, authGuard, destinoSeguro, invitadoGuard } from './guards';
import { Notificaciones } from './notificaciones';
import { Sesion } from './sesion';

describe('guards', () => {
  let sesion: Sesion;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    sesion = TestBed.inject(Sesion);
    router = TestBed.inject(Router);
  });

  function ejecutar(guard: typeof authGuard, url = '/recetas') {
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );
  }

  describe('authGuard', () => {
    it('deja pasar con sesión', () => {
      sesion.iniciar(loginDePrueba());
      expect(ejecutar(authGuard)).toBe(true);
    });

    it('sin sesión lleva al login con returnUrl', () => {
      const resultado = ejecutar(authGuard, '/recetas?page=2') as UrlTree;
      expect(router.serializeUrl(resultado)).toBe('/login?returnUrl=%2Frecetas%3Fpage%3D2');
    });

    it('sin sesión, desde la portada, lleva al login sin returnUrl', () => {
      const resultado = ejecutar(authGuard, '/') as UrlTree;
      expect(router.serializeUrl(resultado)).toBe('/login');
    });

    it('con el token caducado cierra la sesión y lleva al login', () => {
      sesion.iniciar(loginDePrueba(['USER'], -1));
      const resultado = ejecutar(authGuard) as UrlTree;
      expect(router.serializeUrl(resultado)).toContain('/login');
      expect(sesion.autenticado()).toBe(false);
    });
  });

  describe('adminGuard', () => {
    it('deja pasar a un administrador', () => {
      sesion.iniciar(loginDePrueba(['USER', 'ADMIN']));
      expect(ejecutar(adminGuard)).toBe(true);
    });

    it('lleva a la portada y avisa a quien no es administrador', () => {
      sesion.iniciar(loginDePrueba(['USER']));
      const resultado = ejecutar(adminGuard) as UrlTree;
      expect(router.serializeUrl(resultado)).toBe('/');
      expect(TestBed.inject(Notificaciones).avisos()[0].tipo).toBe('aviso');
    });
  });

  describe('invitadoGuard', () => {
    it('deja pasar sin sesión', () => {
      expect(ejecutar(invitadoGuard, '/login')).toBe(true);
    });

    it('con sesión lleva a la portada', () => {
      sesion.iniciar(loginDePrueba());
      const resultado = ejecutar(invitadoGuard, '/login') as UrlTree;
      expect(router.serializeUrl(resultado)).toBe('/');
    });
  });

  describe('destinoSeguro', () => {
    it.each([
      ['/recetas/3', '/recetas/3'],
      ['/mi-alimentacion?x=1', '/mi-alimentacion?x=1'],
      [undefined, '/'],
      ['', '/'],
      ['https://malicioso.example', '/'],
      ['//malicioso.example', '/'],
      ['/\\malicioso.example', '/'],
      ['/login?returnUrl=%2F', '/'],
    ])('%s → %s', (entrada, esperado) => {
      expect(destinoSeguro(entrada)).toBe(esperado);
    });
  });
});
