import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { paginaDe } from '../../testing/datos';
import { loginDePrueba } from '../../testing/jwt';
import { UsuarioAdmin } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { GestionUsuarios, nombreCompleto } from './gestion-usuarios';

function usuario(cambios: Partial<UsuarioAdmin> = {}): UsuarioAdmin {
  return {
    id: 'lucia',
    nombre: 'Lucía',
    apellido1: 'Fernández',
    apellido2: 'Ruiz',
    email: 'lucia@toca.local',
    admin: false,
    bloqueado: false,
    ...cambios,
  };
}

describe('GestionUsuarios', () => {
  let fixture: ComponentFixture<GestionUsuarios>;
  let servidor: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servidor = TestBed.inject(HttpTestingController);
    TestBed.inject(Sesion).iniciar({
      ...loginDePrueba(['USER', 'ADMIN']),
      usuario: {
        id: 'admin',
        nombre: 'Administrador',
        roles: ['USER', 'ADMIN'],
        emailVerificado: true,
      },
    });
    fixture = TestBed.createComponent(GestionUsuarios);
    await fixture.whenStable();
    servidor.expectOne('/api/admin/usuarios?page=0&size=10').flush(
      paginaDe(
        [
          usuario({
            id: 'admin',
            nombre: 'Administrador',
            apellido1: 'Toca',
            apellido2: null,
            admin: true,
          }),
          usuario(),
        ],
        { tamano: 10 },
      ),
    );
    await fixture.whenStable();
  });

  afterEach(() => servidor.verify());

  function html(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function interruptor(id: string): HTMLButtonElement {
    return html().querySelector(`[aria-label="Administrador: ${id}"]`) as HTMLButtonElement;
  }

  function bloqueo(id: string): HTMLButtonElement {
    return html().querySelector(`[aria-label="Bloqueado: ${id}"]`) as HTMLButtonElement;
  }

  it('une nombre y apellidos', () => {
    expect(nombreCompleto(usuario())).toBe('Lucía Fernández Ruiz');
    expect(nombreCompleto(usuario({ apellido2: null }))).toBe('Lucía Fernández');
  });

  it('muestra la tabla con ID, nombre, email y el interruptor de administrador', () => {
    const cabeceras = Array.from(html().querySelectorAll('th')).map((th) => th.textContent?.trim());
    expect(cabeceras).toEqual(['ID', 'Nombre', 'Email', 'Administrador', 'Bloqueado']);
    const filas = html().querySelectorAll('tbody tr');
    expect(filas.length).toBe(2);
    expect(filas[1].textContent).toContain('Lucía Fernández Ruiz');
    expect(filas[1].textContent).toContain('lucia@toca.local');
    expect(interruptor('lucia').getAttribute('aria-pressed')).toBe('false');
    expect(interruptor('admin').getAttribute('aria-pressed')).toBe('true');
  });

  it('no deja que el administrador conectado se quite el rol', () => {
    expect(interruptor('admin').disabled).toBe(true);
    expect(html().querySelector('tbody tr')?.textContent).toContain('Tú');
    fixture.componentInstance.alternarAdmin(usuario({ id: 'admin', admin: true }));
    servidor.expectNone((p) => p.method === 'PUT');
  });

  it('da el rol de administrador y actualiza la fila', async () => {
    interruptor('lucia').click();
    const peticion = servidor.expectOne('/api/admin/usuarios/lucia/admin');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ admin: true });
    peticion.flush(usuario({ admin: true }));
    await fixture.whenStable();

    expect(interruptor('lucia').getAttribute('aria-pressed')).toBe('true');
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toContain('lucia ya es administrador');
  });

  it('avisa si no se puede quitar el rol porque es el último administrador', () => {
    fixture.componentInstance.alternarAdmin(usuario({ admin: true }));
    servidor.expectOne('/api/admin/usuarios/lucia/admin').flush(
      {
        codigo: 'ULTIMO_ADMINISTRADOR',
        mensaje: 'Debe quedar al menos un administrador',
        errores: {},
      },
      { status: 409, statusText: 'Conflict' },
    );
    expect(TestBed.inject(Notificaciones).avisos()[0]).toEqual(
      expect.objectContaining({ tipo: 'error', texto: 'Debe quedar al menos un administrador' }),
    );
  });

  it('si ya no es administrador en el servidor (403) lo explica', async () => {
    interruptor('lucia').click();
    servidor
      .expectOne('/api/admin/usuarios/lucia/admin')
      .flush(
        { codigo: 'ACCESO_DENEGADO', mensaje: 'No', errores: {} },
        { status: 403, statusText: 'Forbidden' },
      );
    await fixture.whenStable();
    expect(html().querySelector('.alert-warning')?.textContent).toContain(
      'Ya no tienes permisos de administrador',
    );
  });

  it('bloquea a un usuario, lo marca y lo avisa', async () => {
    bloqueo('lucia').click();
    const peticion = servidor.expectOne('/api/admin/usuarios/lucia/bloqueado');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ bloqueado: true });
    peticion.flush(usuario({ bloqueado: true }));
    await fixture.whenStable();

    expect(bloqueo('lucia').getAttribute('aria-pressed')).toBe('true');
    expect(html().querySelectorAll('tbody tr')[1].textContent).toContain('Bloqueado');
    expect(TestBed.inject(Notificaciones).avisos()[0].texto).toContain('lucia está bloqueado');
    // Bloqueado, no se le puede hacer administrador
    expect(interruptor('lucia').disabled).toBe(true);
    expect(interruptor('lucia').title).toBe('Desbloquéalo antes de hacerlo administrador');
  });

  it('no deja bloquearse a uno mismo ni bloquear a un administrador', () => {
    expect(bloqueo('admin').disabled).toBe(true);
    expect(bloqueo('admin').title).toBe('No puedes bloquearte a ti mismo');
    fixture.componentInstance.alternarBloqueo(usuario({ id: 'otra', admin: true }));
    servidor.expectNone((p) => p.method === 'PUT');
    expect(fixture.componentInstance.motivoSinBloqueo(usuario({ admin: true }))).toBe(
      'Quítale antes el rol de administrador',
    );
  });

  it('busca por texto y Restablecer vuelve a la lista completa', () => {
    const componente = fixture.componentInstance;
    componente.buscador.setValue({ q: ' luc ' });
    componente.buscar();
    servidor.expectOne('/api/admin/usuarios?page=0&size=10&q=luc').flush(paginaDe([usuario()]));

    componente.restablecer();
    servidor.expectOne('/api/admin/usuarios?page=0&size=10').flush(paginaDe([]));
    expect(componente.buscador.controls.q.value).toBe('');
  });
});
