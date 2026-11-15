import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { loginDePrueba } from '../../testing/jwt';
import { Perfil } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { MiPerfil } from './mi-perfil';
import { ModalBaja } from './modal-baja';
import { ModalDesvincularGoogle } from './modal-desvincular-google';

const PERFIL: Perfil = {
  id: 'lucia',
  nombre: 'Lucía',
  apellido1: 'Fernández',
  apellido2: null,
  email: 'lucia@toca.local',
  roles: ['USER'],
  google: false,
  emailVerificado: true,
  emailPendiente: null,
};

const activo = { close: vi.fn(), dismiss: vi.fn() };

describe('MiPerfil', () => {
  let fixture: ComponentFixture<MiPerfil>;
  let componente: MiPerfil;
  let servidor: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: NgbActiveModal, useValue: activo },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
    TestBed.inject(Sesion).iniciar(loginDePrueba());
    fixture = TestBed.createComponent(MiPerfil);
    componente = fixture.componentInstance;
    await fixture.whenStable();
    servidor.expectOne('/api/usuarios/me').flush(PERFIL);
    await fixture.whenStable();
  });

  afterEach(() => servidor.verify());

  function html(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('descarga los datos como fichero sin pasar por el service worker', async () => {
    const crearUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:datos');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    componente.descargarDatos();
    const peticion = servidor.expectOne('/api/usuarios/me/datos');
    expect(peticion.request.headers.get('ngsw-bypass')).toBe('true');
    expect(peticion.request.responseType).toBe('blob');
    peticion.flush(new Blob(['{}'], { type: 'application/json' }));

    expect(crearUrl).toHaveBeenCalled();
    expect(clic).toHaveBeenCalled();
    expect((clic.mock.contexts[0] as HTMLAnchorElement).download).toBe('alimentarse-lucia.json');
  });

  it('tras darse de baja cierra la sesión y lleva al login', async () => {
    vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
      componentInstance: {},
      result: Promise.resolve(true),
    } as never);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    componente.datos.markAsDirty();

    Array.from(html().querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Eliminar mi cuenta'))!
      .click();
    await fixture.whenStable();

    expect(TestBed.inject(Sesion).autenticado()).toBe(false);
    expect(componente.hayCambiosSinGuardar()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/login');
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toContain('Tu cuenta se ha eliminado');
  });

  describe('ModalDesvincularGoogle', () => {
    beforeEach(() => activo.close.mockReset());

    it('envía la contraseña y cierra con el perfil actualizado', async () => {
      const modal = TestBed.createComponent(ModalDesvincularGoogle);
      await modal.whenStable();
      modal.componentInstance.confirmar();
      servidor.expectNone('/api/usuarios/me/desvincular-google');

      modal.componentInstance.formulario.controls.password.setValue('demo1234');
      modal.componentInstance.confirmar();
      const peticion = servidor.expectOne('/api/usuarios/me/desvincular-google');
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({ password: 'demo1234' });
      peticion.flush({ ...PERFIL, google: false });

      expect(activo.close).toHaveBeenCalledWith({ ...PERFIL, google: false });
    });

    it('una contraseña incorrecta se marca en el campo y otro error, arriba', async () => {
      const modal = TestBed.createComponent(ModalDesvincularGoogle);
      await modal.whenStable();
      modal.componentInstance.formulario.controls.password.setValue('otra');
      modal.componentInstance.confirmar();
      servidor.expectOne('/api/usuarios/me/desvincular-google').flush(
        {
          codigo: 'PASSWORD_INCORRECTA',
          mensaje: 'La contraseña actual no es correcta.',
          errores: { password: 'La contraseña actual no es correcta' },
        },
        { status: 400, statusText: 'Bad Request' },
      );
      await modal.whenStable();
      expect(modal.componentInstance.formulario.controls.password.errors).toEqual({
        servidor: 'La contraseña actual no es correcta',
      });

      modal.componentInstance.formulario.controls.password.setValue('demo1234');
      modal.componentInstance.confirmar();
      servidor
        .expectOne('/api/usuarios/me/desvincular-google')
        .flush(
          { codigo: 'DEMASIADOS_INTENTOS', mensaje: 'Demasiados intentos.', errores: {} },
          { status: 429, statusText: 'Too Many Requests' },
        );
      await modal.whenStable();
      expect(modal.nativeElement.querySelector('[role=alert]').textContent).toContain(
        'Demasiados intentos.',
      );
      expect(activo.close).not.toHaveBeenCalled();
    });
  });

  describe('ModalBaja', () => {
    beforeEach(() => activo.close.mockReset());

    it('envía la contraseña y cierra al eliminarse la cuenta', async () => {
      const modal = TestBed.createComponent(ModalBaja);
      await modal.whenStable();
      modal.componentInstance.formulario.controls.password.setValue('demo1234');
      modal.componentInstance.confirmar();

      const peticion = servidor.expectOne('/api/usuarios/me/baja');
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({ password: 'demo1234' });
      peticion.flush(null, { status: 204, statusText: 'No Content' });

      expect(activo.close).toHaveBeenCalledWith(true);
    });

    it('una contraseña incorrecta se marca en el campo', async () => {
      const modal = TestBed.createComponent(ModalBaja);
      await modal.whenStable();
      modal.componentInstance.formulario.controls.password.setValue('otra');
      modal.componentInstance.confirmar();
      servidor.expectOne('/api/usuarios/me/baja').flush(
        {
          codigo: 'PASSWORD_INCORRECTA',
          mensaje: 'La contraseña no es correcta.',
          errores: { password: 'La contraseña no es correcta' },
        },
        { status: 400, statusText: 'Bad Request' },
      );
      await modal.whenStable();

      expect(modal.componentInstance.formulario.controls.password.errors).toEqual({
        servidor: 'La contraseña no es correcta',
      });
      expect(activo.close).not.toHaveBeenCalled();
    });

    it('si es el último administrador lo explica', async () => {
      const modal = TestBed.createComponent(ModalBaja);
      await modal.whenStable();
      modal.componentInstance.formulario.controls.password.setValue('demo1234');
      modal.componentInstance.confirmar();
      servidor.expectOne('/api/usuarios/me/baja').flush(
        {
          codigo: 'ULTIMO_ADMINISTRADOR',
          mensaje: 'Eres el único administrador: nombra a otro antes de darte de baja.',
          errores: {},
        },
        { status: 409, statusText: 'Conflict' },
      );
      await modal.whenStable();

      expect(
        (modal.nativeElement as HTMLElement).querySelector('.alert-danger')?.textContent,
      ).toContain('Eres el único administrador');
    });
  });

  describe('con la cuenta vinculada con Google', () => {
    beforeEach(async () => {
      componente.perfil.set({ ...PERFIL, google: true });
      await fixture.whenStable();
    });

    function botonEnlace(): HTMLButtonElement {
      return [...fixture.nativeElement.querySelectorAll('button')].find((b: HTMLButtonElement) =>
        b.textContent?.includes('Enviarme el enlace'),
      ) as HTMLButtonElement;
    }

    it('lo dice en sus datos', () => {
      expect(fixture.nativeElement.textContent).toContain(
        'Vinculada: también entras con tu cuenta de Google',
      );
    });

    it('al cambiar el email recuerda cómo crear una contraseña', async () => {
      componente.datos.patchValue({ email: 'lucia@nuevo.es' });
      await fixture.whenStable();
      expect(html().querySelector('#perfil-password-email-ayuda')?.textContent).toContain(
        'créala antes con «Enviarme el enlace»',
      );
    });

    it('desvincula Google tras confirmarlo en el modal', async () => {
      vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
        componentInstance: {},
        result: Promise.resolve({ ...PERFIL, google: false }),
      } as never);

      Array.from(html().querySelectorAll('button'))
        .find((b) => b.textContent?.includes('Desvincular Google'))!
        .click();
      await fixture.whenStable();

      expect(componente.perfil()?.google).toBe(false);
      expect(html().textContent).not.toContain('Desvincular Google');
      expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe(
        'Tu cuenta ya no está vinculada con Google.',
      );
    });

    it('manda a su email el enlace para crear una contraseña', async () => {
      const exito = vi.spyOn(TestBed.inject(Notificaciones), 'exito');
      botonEnlace().click();
      await fixture.whenStable();
      expect(botonEnlace().disabled).toBe(true);

      const peticion = servidor.expectOne('/api/auth/password/olvido');
      expect(peticion.request.body).toEqual({ email: 'lucia@toca.local' });
      peticion.flush({ mensaje: 'Enviado' });
      await fixture.whenStable();

      expect(exito).toHaveBeenCalledWith('Te hemos enviado el enlace a lucia@toca.local.');
      expect(botonEnlace().disabled).toBe(false);
    });

    it('si se ha pedido demasiadas veces lo explica', () => {
      const error = vi.spyOn(TestBed.inject(Notificaciones), 'error');
      botonEnlace().click();
      servidor
        .expectOne('/api/auth/password/olvido')
        .flush(
          { codigo: 'DEMASIADOS_INTENTOS', mensaje: 'Demasiados intentos.', errores: {} },
          { status: 429, statusText: 'Too Many Requests' },
        );

      expect(error).toHaveBeenCalledWith('Demasiados intentos.');
      // Mientras se envía, otro clic no manda otro correo
      componente.enviandoEnlace.set(true);
      componente.enviarEnlacePassword();
      servidor.expectNone('/api/auth/password/olvido');
    });
  });

  it('muestra los datos actuales y el usuario, que no se puede cambiar', () => {
    expect(componente.datos.getRawValue()).toEqual({
      nombre: 'Lucía',
      apellido1: 'Fernández',
      apellido2: '',
      email: 'lucia@toca.local',
      password: '',
    });
    expect(html().querySelector('dl')?.textContent).toContain('lucia');
    expect(html().querySelector('dl')?.textContent).toContain('Usuario');
    expect(componente.hayCambiosSinGuardar()).toBe(false);
  });

  it('guardar está deshabilitado hasta que se cambia algo', async () => {
    const boton = () =>
      Array.from(html().querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Guardar los datos'),
      )!;
    expect(boton().disabled).toBe(true);

    const nombre = html().querySelector('#perfil-nombre') as HTMLInputElement;
    nombre.value = 'Lucía María';
    nombre.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(boton().disabled).toBe(false);
    expect(componente.hayCambiosSinGuardar()).toBe(true);
  });

  it('guarda los datos y actualiza el nombre de la cabecera', async () => {
    componente.datos.setValue({
      nombre: ' Lucía María ',
      apellido1: 'Fernández',
      apellido2: ' Ruiz ',
      email: 'lucia@nuevo.es',
      password: 'demo1234',
    });
    componente.datos.markAsDirty();

    componente.guardarDatos();
    const peticion = servidor.expectOne('/api/usuarios/me');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      nombre: 'Lucía María',
      apellido1: 'Fernández',
      apellido2: 'Ruiz',
      email: 'lucia@nuevo.es',
      password: 'demo1234',
    });
    peticion.flush({
      ...PERFIL,
      nombre: 'Lucía María',
      apellido2: 'Ruiz',
      email: 'lucia@nuevo.es',
    });

    expect(TestBed.inject(Sesion).usuario()?.nombre).toBe('Lucía María');
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe('Datos guardados.');
    expect(componente.datos.pristine).toBe(true);
    // Con el email ya guardado, deja de pedir la contraseña
    expect(componente.cambiaEmail()).toBe(false);
    expect(componente.datos.controls.password.value).toBe('');
  });

  describe('confirmación del email (D-088)', () => {
    function botonCon(texto: string): HTMLButtonElement {
      return Array.from(html().querySelectorAll('button')).find((b) =>
        b.textContent?.includes(texto),
      )!;
    }

    it('con el email confirmado lo dice', () => {
      expect(html().querySelector('#perfil-email-estado')?.textContent).toContain('Confirmado');
    });

    it('sin confirmar permite enviar el enlace', async () => {
      componente.perfil.set({ ...PERFIL, emailVerificado: false });
      await fixture.whenStable();
      const exito = vi.spyOn(TestBed.inject(Notificaciones), 'exito');

      botonCon('Enviar el enlace').click();
      componente.enviarConfirmacion();
      // Mientras se envía, otro clic no manda otro correo
      const peticion = servidor.expectOne('/api/usuarios/me/email/confirmacion');
      peticion.flush({ email: 'lucia@toca.local' });

      expect(exito).toHaveBeenCalledWith('Te hemos enviado el enlace a lucia@toca.local.');
    });

    it('con un cambio pendiente lo explica, permite reenviarlo o cancelarlo', async () => {
      componente.perfil.set({ ...PERFIL, emailPendiente: 'lucia@nuevo.es' });
      await fixture.whenStable();
      expect(html().querySelector('#perfil-email-estado')?.textContent).toContain(
        'Cambio pendiente: te hemos enviado un enlace a',
      );
      expect(html().querySelector('#perfil-email-estado')?.textContent).toContain('lucia@nuevo.es');

      const error = vi.spyOn(TestBed.inject(Notificaciones), 'error');
      botonCon('Reenviar el enlace').click();
      servidor
        .expectOne('/api/usuarios/me/email/confirmacion')
        .flush(
          { codigo: 'DEMASIADOS_INTENTOS', mensaje: 'Demasiados intentos.', errores: {} },
          { status: 429, statusText: 'Too Many Requests' },
        );
      expect(error).toHaveBeenCalledWith('Demasiados intentos.');

      botonCon('Cancelar el cambio').click();
      const cancelar = servidor.expectOne('/api/usuarios/me/email/pendiente');
      expect(cancelar.request.method).toBe('DELETE');
      cancelar.flush(PERFIL);
      await fixture.whenStable();

      expect(componente.perfil()?.emailPendiente).toBeNull();
      expect(TestBed.inject(Notificaciones).avisos().at(-1)?.texto).toBe(
        'Cambio de email cancelado.',
      );
    });

    it('si no se puede cancelar, lo dice', () => {
      const error = vi.spyOn(TestBed.inject(Notificaciones), 'error');
      componente.cancelarCambioEmail();
      servidor
        .expectOne('/api/usuarios/me/email/pendiente')
        .flush(
          { codigo: 'NO_ENCONTRADO', mensaje: 'El usuario no existe.', errores: {} },
          { status: 404, statusText: 'Not Found' },
        );
      expect(error).toHaveBeenCalledWith('El usuario no existe.');
    });

    it('el perfil actualiza el aviso de la cabecera', () => {
      const sesion = TestBed.inject(Sesion);
      expect(sesion.usuario()?.emailVerificado).toBe(true);
      componente.cargar();
      servidor.expectOne('/api/usuarios/me').flush({ ...PERFIL, emailVerificado: false });
      expect(sesion.usuario()?.emailVerificado).toBe(false);
    });

    it('al guardar un email nuevo dice adónde ha ido el enlace', () => {
      componente.datos.setValue({
        nombre: 'Lucía',
        apellido1: 'Fernández',
        apellido2: '',
        email: 'lucia@nuevo.es',
        password: 'demo1234',
      });
      componente.datos.markAsDirty();
      componente.guardarDatos();
      servidor.expectOne('/api/usuarios/me').flush({ ...PERFIL, emailPendiente: 'lucia@nuevo.es' });

      expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe(
        'Datos guardados. Te hemos enviado un enlace a lucia@nuevo.es para confirmar el cambio.',
      );
      // El formulario vuelve al email de la cuenta, que sigue siendo el anterior
      expect(componente.datos.controls.email.value).toBe('lucia@toca.local');
      expect(componente.cambiaEmail()).toBe(false);
    });
  });

  describe('cambiar el email', () => {
    async function escribirEmail(valor: string) {
      const email = html().querySelector('#perfil-email') as HTMLInputElement;
      email.value = valor;
      email.dispatchEvent(new Event('input'));
      await fixture.whenStable();
    }

    it('pide la contraseña actual solo si el email es otro', async () => {
      expect(html().querySelector('#perfil-password-email')).toBeNull();

      await escribirEmail('lucia@nuevo.es');
      expect(html().querySelector('#perfil-password-email')).not.toBeNull();
      expect(html().querySelector('#perfil-password-email-ayuda')?.textContent).toContain(
        'Te enviaremos un enlace al email nuevo',
      );
      expect(componente.datos.invalid).toBe(true);

      componente.datos.controls.password.setValue('demo1234');
      expect(componente.datos.valid).toBe(true);

      // Solo con otras mayúsculas es el mismo email
      await escribirEmail('Lucia@Toca.local');
      expect(html().querySelector('#perfil-password-email')).toBeNull();
      expect(componente.datos.controls.password.value).toBe('');
      expect(componente.datos.valid).toBe(true);
    });

    it('sin cambiar el email no envía ninguna contraseña', () => {
      componente.datos.patchValue({ nombre: 'Lucía María' });
      componente.datos.markAsDirty();
      componente.guardarDatos();

      const peticion = servidor.expectOne('/api/usuarios/me');
      expect(peticion.request.body.password).toBeNull();
      peticion.flush({ ...PERFIL, nombre: 'Lucía María' });
    });

    it('una contraseña incorrecta se marca en su campo', async () => {
      await escribirEmail('lucia@nuevo.es');
      componente.datos.controls.password.setValue('otra');
      componente.guardarDatos();
      servidor.expectOne('/api/usuarios/me').flush(
        {
          codigo: 'PASSWORD_INCORRECTA',
          mensaje: 'La contraseña actual no es correcta.',
          errores: { password: 'La contraseña actual no es correcta' },
        },
        { status: 400, statusText: 'Bad Request' },
      );
      await fixture.whenStable();

      expect(html().querySelector('#perfil-password-email-error')?.textContent).toContain(
        'La contraseña actual no es correcta',
      );
    });
  });

  it('si el email ya lo usa otra cuenta lo indica junto al campo', async () => {
    componente.datos.patchValue({ email: 'marcos@toca.local', password: 'demo1234' });
    componente.guardarDatos();
    servidor.expectOne('/api/usuarios/me').flush(
      {
        codigo: 'CONFLICTO',
        mensaje: 'Revisa los datos',
        errores: { email: 'Ya hay una cuenta con ese email' },
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    expect(html().querySelector('#perfil-email-error')?.textContent).toContain(
      'Ya hay una cuenta con ese email',
    );
  });

  it('cambia la contraseña y vacía el formulario', () => {
    componente.password.setValue({
      actual: 'demo1234',
      nueva: 'otra-clave',
      confirmacion: 'otra-clave',
    });

    componente.cambiarPassword();
    const peticion = servidor.expectOne('/api/usuarios/me/password');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ actual: 'demo1234', nueva: 'otra-clave' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });

    expect(componente.password.getRawValue()).toEqual({ actual: '', nueva: '', confirmacion: '' });
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toBe('Contraseña cambiada.');
  });

  it('exige que la contraseña nueva y su confirmación coincidan', () => {
    componente.password.setValue({ actual: 'demo1234', nueva: 'otra-clave', confirmacion: 'otra' });
    componente.cambiarPassword();
    servidor.expectNone('/api/usuarios/me/password');
    expect(componente.password.controls.confirmacion.hasError('noCoinciden')).toBe(true);
  });

  it('si la contraseña actual no es correcta lo indica junto al campo', async () => {
    componente.password.setValue({
      actual: 'mala',
      nueva: 'otra-clave',
      confirmacion: 'otra-clave',
    });
    componente.cambiarPassword();
    servidor.expectOne('/api/usuarios/me/password').flush(
      {
        codigo: 'PASSWORD_INCORRECTA',
        mensaje: 'La contraseña actual no es correcta',
        errores: { actual: 'La contraseña actual no es correcta' },
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    expect(html().querySelector('#perfil-actual-error')?.textContent).toContain(
      'La contraseña actual no es correcta',
    );
  });
});
