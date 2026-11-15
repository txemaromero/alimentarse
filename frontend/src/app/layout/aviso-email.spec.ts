import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { loginDePrueba } from '../../testing/jwt';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { AvisoEmail } from './aviso-email';

@Component({ template: '' })
class Vacia {}

describe('AvisoEmail', () => {
  let fixture: ComponentFixture<AvisoEmail>;
  let servidor: HttpTestingController;
  let sesion: Sesion;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: '', component: Vacia },
          { path: 'confirmar-email', component: Vacia },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(Sesion);
    fixture = TestBed.createComponent(AvisoEmail);
    await fixture.whenStable();
  });

  afterEach(() => servidor.verify());

  function texto(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function boton(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button') as HTMLButtonElement;
  }

  async function sinConfirmar() {
    const respuesta = loginDePrueba();
    sesion.iniciar({ ...respuesta, usuario: { ...respuesta.usuario, emailVerificado: false } });
    await fixture.whenStable();
  }

  it('sin sesión o con el email confirmado no aparece', async () => {
    expect(texto()).toBe('');
    sesion.iniciar(loginDePrueba());
    await fixture.whenStable();
    expect(texto()).toBe('');
  });

  it('con el email sin confirmar lo recuerda, salvo en la pantalla de confirmación', async () => {
    await sinConfirmar();
    expect(texto()).toContain('Tu email está sin confirmar');
    expect(fixture.nativeElement.querySelector('a[href="/mi-perfil"]')).not.toBeNull();

    await TestBed.inject(Router).navigateByUrl('/confirmar-email');
    await fixture.whenStable();
    expect(texto()).toBe('');
  });

  it('envía el enlace otra vez', async () => {
    await sinConfirmar();
    const exito = vi.spyOn(TestBed.inject(Notificaciones), 'exito');

    boton().click();
    await fixture.whenStable();
    expect(boton().disabled).toBe(true);
    const peticion = servidor.expectOne('/api/usuarios/me/email/confirmacion');
    expect(peticion.request.method).toBe('POST');
    peticion.flush({ email: 'lucia@toca.local' });
    await fixture.whenStable();

    expect(exito).toHaveBeenCalledWith('Te hemos enviado el enlace a lucia@toca.local.');
    expect(boton().disabled).toBe(false);
  });

  it('si ya lo confirmó en otro dispositivo, deja de avisar', async () => {
    await sinConfirmar();
    const info = vi.spyOn(TestBed.inject(Notificaciones), 'info');

    boton().click();
    servidor
      .expectOne('/api/usuarios/me/email/confirmacion')
      .flush(
        { codigo: 'EMAIL_YA_CONFIRMADO', mensaje: 'Tu email ya está confirmado.', errores: {} },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(info).toHaveBeenCalledWith('Tu email ya está confirmado.');
    expect(sesion.usuario()?.emailVerificado).toBe(true);
    expect(texto()).toBe('');
  });

  it('si lo ha pedido demasiadas veces, lo explica', async () => {
    await sinConfirmar();
    const error = vi.spyOn(TestBed.inject(Notificaciones), 'error');

    boton().click();
    servidor
      .expectOne('/api/usuarios/me/email/confirmacion')
      .flush(
        { codigo: 'DEMASIADOS_INTENTOS', mensaje: 'Demasiados intentos.', errores: {} },
        { status: 429, statusText: 'Too Many Requests' },
      );

    expect(error).toHaveBeenCalledWith('Demasiados intentos.');
    expect(sesion.usuario()?.emailVerificado).toBe(false);
  });
});
