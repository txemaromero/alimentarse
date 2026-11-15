import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { Cabecera } from './cabecera';

describe('Cabecera', () => {
  let fixture: ComponentFixture<Cabecera>;
  let sesion: Sesion;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Cabecera],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    sesion = TestBed.inject(Sesion);
  });

  async function crear() {
    fixture = TestBed.createComponent(Cabecera);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  function textos(html: HTMLElement, selector: string): string[] {
    return Array.from(html.querySelectorAll(selector)).map((e) => e.textContent!.trim());
  }

  it('sin sesión ofrece iniciar sesión y registrarse', async () => {
    const html = await crear();
    expect(textos(html, '.nav-link')).toEqual(['Iniciar sesión', 'Registrarse']);
  });

  it('con sesión muestra el menú principal y el saludo', async () => {
    sesion.iniciar(loginDePrueba(['USER'], 3600, 'Marcos'));
    const html = await crear();

    expect(textos(html, 'a.nav-link')).toEqual([
      'Inicio',
      'Recetas',
      'Ingredientes',
      'Qué toca hoy',
    ]);
    expect(html.textContent).toContain('Bienvenido, Marcos');
    expect(html.textContent).not.toContain('Administrador');
  });

  it('a un administrador le muestra el menú Administrador', async () => {
    sesion.iniciar(loginDePrueba(['USER', 'ADMIN']));
    const html = await crear();
    expect(html.querySelector('#menu-admin')?.textContent).toContain('Administrador');
  });

  it('el menú del usuario lleva a perfil, recetas, alimentación y cierre de sesión', async () => {
    sesion.iniciar(loginDePrueba());
    const html = await crear();

    (html.querySelector('#menu-usuario') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(textos(html, '[ngbDropdownItem]')).toEqual([
      'Mi perfil',
      'Mis recetas',
      'Recetas que me gustan',
      'Mi alimentación',
      'Planificador',
      'Cerrar sesión',
    ]);
  });

  it('cerrar sesión borra la sesión y lleva al login', async () => {
    sesion.iniciar(loginDePrueba());
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    await crear();

    fixture.componentInstance.cerrarSesion();
    // Antes, los avisos de este navegador (D-078)
    await new Promise((r) => setTimeout(r));
    TestBed.inject(HttpTestingController).expectOne('/api/auth/logout').flush(null);

    expect(sesion.autenticado()).toBe(false);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    expect(TestBed.inject(Notificaciones).avisos()[0].tipo).toBe('info');
  });

  it('sin conexión con el servidor, la sesión se cierra igual en este dispositivo', async () => {
    sesion.iniciar(loginDePrueba());
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await crear();

    fixture.componentInstance.cerrarSesion();
    await new Promise((r) => setTimeout(r));
    TestBed.inject(HttpTestingController)
      .expectOne('/api/auth/logout')
      .error(new ProgressEvent('error'));

    expect(sesion.autenticado()).toBe(false);
  });

  it('el botón de menú indica si está desplegado', async () => {
    const html = await crear();
    const boton = html.querySelector('.navbar-toggler') as HTMLButtonElement;
    expect(boton.getAttribute('aria-expanded')).toBe('false');

    boton.click();
    await fixture.whenStable();

    expect(boton.getAttribute('aria-expanded')).toBe('true');
  });
});
