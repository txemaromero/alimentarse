import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Router, TitleStrategy, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { loginDePrueba } from '../testing/jwt';
import { App } from './app';
import { routes } from './app.routes';
import { Sesion } from './core/sesion';
import { TituloPagina } from './core/titulo-pagina';

describe('App', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TitleStrategy, useClass: TituloPagina },
      ],
    });
  });

  it('tiene cabecera, contenido principal y pie', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('app-cabecera')).toBeTruthy();
    expect(html.querySelector('main#contenido')).toBeTruthy();
    expect(html.querySelector('app-pie')?.textContent).toContain('Información legal');
  });

  it('sin sesión, la portada lleva al login', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');
    expect(harness.routeNativeElement?.textContent).toContain('Iniciar sesión');
    expect(TestBed.inject(Title).getTitle()).toBe('Iniciar sesión · Alimentarse');
  });

  it('una ruta protegida guarda la página pedida en returnUrl', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mi-alimentacion');
    expect(TestBed.inject(Router).url).toBe('/login?returnUrl=%2Fmi-alimentacion');
  });

  it('las páginas informativas son públicas', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/informacion-legal');
    expect(harness.routeNativeElement?.textContent).toContain('Datos personales');
  });

  it('las recetas se cargan bajo demanda con su título', async () => {
    TestBed.inject(Sesion).iniciar(loginDePrueba());
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/recetas');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain('Recetas');
    expect(TestBed.inject(Title).getTitle()).toBe('Recetas · Alimentarse');
  });

  it('un administrador entra en la gestión de usuarios', async () => {
    TestBed.inject(Sesion).iniciar(loginDePrueba(['USER', 'ADMIN']));
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/admin');
    expect(TestBed.inject(Router).url).toBe('/admin/usuarios');
    expect(TestBed.inject(Title).getTitle()).toBe('Gestión de usuarios · Alimentarse');
  });

  it('la dirección antigua de «¿Qué toca hoy?» lleva a la nueva (D-066)', async () => {
    TestBed.inject(Sesion).iniciar(loginDePrueba());
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/que-como-hoy');
    expect(TestBed.inject(Router).url).toBe('/que-toca-hoy');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain(
      '¿Qué toca hoy?',
    );
    expect(TestBed.inject(Title).getTitle()).toBe('Qué toca hoy · Alimentarse');
  });

  it('un usuario sin rol ADMIN no entra en la administración', async () => {
    TestBed.inject(Sesion).iniciar(loginDePrueba(['USER']));
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/admin/usuarios');
    expect(TestBed.inject(Title).getTitle()).toBe('Inicio · Alimentarse');
  });

  it('una ruta desconocida muestra la página 404', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/no-existe');
    expect(harness.routeNativeElement?.textContent).toContain('Página no encontrada');
  });
});
