import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { aceptarConfirmacion } from '../../testing/confirmacion';
import { detalleDePrueba, paginaDe, valoracionDePrueba } from '../../testing/datos';
import { loginDePrueba } from '../../testing/jwt';
import { RecetaDetalle, ResumenValoraciones, Valoracion } from '../core/modelos';
import { Notificaciones } from '../core/notificaciones';
import { Sesion } from '../core/sesion';
import { Valoraciones } from './valoraciones';

registerLocaleData(localeEs);

describe('Valoraciones', () => {
  let fixture: ComponentFixture<Valoraciones>;
  let servidor: HttpTestingController;
  let cambios: ResumenValoraciones[];

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'es' },
      ],
    });
    servidor = TestBed.inject(HttpTestingController);
    cambios = [];
  });

  afterEach(() => {
    servidor.verify();
    vi.restoreAllMocks();
  });

  const lista = () => servidor.expectOne((r) => r.url === '/api/recetas/1/valoraciones');

  async function crear(receta: RecetaDetalle = detalleDePrueba(), valoraciones: Valoracion[] = []) {
    fixture = TestBed.createComponent(Valoraciones);
    fixture.componentRef.setInput('receta', receta);
    fixture.componentInstance.cambio.subscribe((c) => cambios.push(c));
    await fixture.whenStable();
    lista().flush(paginaDe(valoraciones, { tamano: 5 }));
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const boton = (html: HTMLElement, texto: string) =>
    [...html.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent!.includes(texto),
    )!;

  it('muestra la nota media y la lista, de la más reciente a la más antigua', async () => {
    const html = await crear(detalleDePrueba({ nota: 4.5, valoraciones: 2 }), [
      valoracionDePrueba({
        id: 8,
        usuario: 'carmen',
        nota: 5,
        comentario: 'Riquísima.\nRepetiré.',
      }),
      valoracionDePrueba({ id: 7, comentario: null }),
    ]);
    // El lector lee solo el texto oculto; lo visible, igual, lleva aria-hidden
    expect(html.querySelector('.resumen .visually-hidden')!.textContent).toBe(
      'Nota media: 4,5 de 5, 2 valoraciones',
    );
    expect(html.querySelector('.resumen')!.textContent).toContain('· 2 valoraciones');
    const items = html.querySelectorAll('li.valoracion');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain('carmen');
    expect(items[0].querySelector('.comentario')!.textContent).toBe('Riquísima.\nRepetiré.');
    expect(items[1].querySelector('.comentario')).toBeNull();
    expect(html.textContent).not.toContain('Ver más');
  });

  it('sin valoraciones lo dice', async () => {
    const html = await crear();
    expect(html.textContent).toContain('Aún no tiene valoraciones.');
  });

  it('«Ver más» añade la página siguiente', async () => {
    const primeras = [1, 2, 3, 4, 5].map((id) => valoracionDePrueba({ id, usuario: `u${id}` }));
    fixture = TestBed.createComponent(Valoraciones);
    fixture.componentRef.setInput('receta', detalleDePrueba());
    await fixture.whenStable();
    lista().flush(paginaDe(primeras, { tamano: 5, totalElementos: 6, totalPaginas: 2 }));
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;

    boton(html, 'Ver más valoraciones').click();
    const segunda = lista();
    expect(segunda.request.params.get('page')).toBe('1');
    segunda.flush(
      paginaDe([valoracionDePrueba({ id: 6, usuario: 'u6' })], {
        pagina: 1,
        tamano: 5,
        totalElementos: 6,
        totalPaginas: 2,
      }),
    );
    await fixture.whenStable();
    expect(html.querySelectorAll('li.valoracion').length).toBe(6);
    expect(html.textContent).not.toContain('Ver más');
  });

  it('su autora no puede valorarla, ni nadie una receta sin publicar', async () => {
    let html = await crear(detalleDePrueba({ esAutor: true }));
    expect(html.querySelector('form')).toBeNull();
    fixture.destroy();
    html = await crear(detalleDePrueba({ publicada: false }));
    expect(html.querySelector('form')).toBeNull();
  });

  it('guarda una valoración nueva y avisa', async () => {
    const html = await crear();
    expect(html.querySelector('#titulo-tu-valoracion')!.textContent).toContain(
      'Valora esta receta',
    );
    const guardar = boton(html, 'Guardar valoración');
    expect(guardar.disabled).toBe(true);

    html.querySelector<HTMLInputElement>('#valoracion-nota-4')!.click();
    const comentario = html.querySelector<HTMLTextAreaElement>('#valoracion-comentario')!;
    comentario.value = '  Muy jugosa.  ';
    comentario.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(html.querySelectorAll('.selector-estrellas svg[data-prefix="fas"]').length).toBe(4);
    expect(html.textContent).toContain('15 de 1000 caracteres');

    guardar.click();
    const peticion = servidor.expectOne('/api/recetas/1/valoracion');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ nota: 4, comentario: 'Muy jugosa.' });
    const mia = valoracionDePrueba({ usuario: 'ana', comentario: 'Muy jugosa.' });
    peticion.flush({ nota: 4, valoraciones: 1, mia });
    lista().flush(paginaDe([mia]));
    await fixture.whenStable();

    expect(cambios).toEqual([{ nota: 4, valoraciones: 1, mia }]);
    expect(TestBed.inject(Notificaciones).avisos().at(-1)?.texto).toBe('Valoración guardada.');
    expect(html.querySelectorAll('li.valoracion').length).toBe(1);
  });

  it('un comentario en blanco se envía como null', async () => {
    const html = await crear(
      detalleDePrueba({ miValoracion: valoracionDePrueba({ comentario: null }) }),
    );
    expect(html.querySelector<HTMLInputElement>('#valoracion-nota-4')!.checked).toBe(true);
    expect(html.querySelector('#titulo-tu-valoracion')!.textContent).toContain('Tu valoración');

    boton(html, 'Guardar valoración').click();
    const peticion = servidor.expectOne('/api/recetas/1/valoracion');
    expect(peticion.request.body).toEqual({ nota: 4, comentario: null });
    peticion.flush({ nota: 4, valoraciones: 1, mia: valoracionDePrueba() });
    lista().flush(paginaDe([]));
  });

  it('muestra los errores del servidor en su campo o en una alerta', async () => {
    const html = await crear(detalleDePrueba({ miValoracion: valoracionDePrueba() }));

    boton(html, 'Guardar valoración').click();
    servidor
      .expectOne('/api/recetas/1/valoracion')
      .flush(
        { codigo: 'VALIDACION', mensaje: 'Revisa', errores: { comentario: 'Demasiado largo' } },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();
    expect(html.querySelector('#valoracion-comentario-error')!.textContent).toContain(
      'Demasiado largo',
    );
    // Al corregir el campo se va el error del servidor
    const comentario = html.querySelector<HTMLTextAreaElement>('#valoracion-comentario')!;
    comentario.value = 'Más corto';
    comentario.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    boton(html, 'Guardar valoración').click();
    servidor
      .expectOne('/api/recetas/1/valoracion')
      .flush(
        { codigo: 'PROPIA_RECETA', mensaje: 'No puedes valorar tu propia receta.', errores: {} },
        { status: 403, statusText: 'Forbidden' },
      );
    await fixture.whenStable();
    expect(html.querySelector('form .alert-danger')!.textContent).toContain(
      'No puedes valorar tu propia receta.',
    );
  });

  it('avisa a su autor si la administración ha ocultado su valoración', async () => {
    const html = await crear(
      detalleDePrueba({ miValoracion: valoracionDePrueba({ oculta: true }) }),
    );
    expect(html.querySelector('form .alert-warning')!.textContent).toContain(
      'La administración ha ocultado tu valoración',
    );
  });

  it('borra la propia tras confirmarlo y deja el foco en el formulario', async () => {
    const html = await crear(detalleDePrueba({ miValoracion: valoracionDePrueba() }));
    const modal = aceptarConfirmacion();

    boton(html, 'Borrar mi valoración').click();
    expect(modal.instancia.opciones!.titulo).toBe('Borrar tu valoración');
    modal.aceptar();
    const peticion = servidor.expectOne('/api/recetas/1/valoracion');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush({ nota: null, valoraciones: 0, mia: null });
    await fixture.whenStable();
    lista().flush(paginaDe([]));
    await fixture.whenStable();
    await new Promise((r) => setTimeout(r));

    expect(cambios).toEqual([{ nota: null, valoraciones: 0, mia: null }]);
    expect(TestBed.inject(Notificaciones).avisos().at(-1)?.texto).toBe('Valoración borrada.');
    expect(document.activeElement?.id).toBe('tu-valoracion');
  });

  it('la administración oculta una valoración y la media se recalcula', async () => {
    TestBed.inject(Sesion).iniciar(loginDePrueba(['USER', 'ADMIN']));
    const html = await crear(detalleDePrueba({ nota: 3, valoraciones: 2 }), [
      valoracionDePrueba({ id: 8, usuario: 'spam', nota: 1 }),
    ]);
    const ocultar = html.querySelector<HTMLButtonElement>(
      'button[aria-label="Ocultar la valoración de spam"]',
    )!;

    ocultar.click();
    const peticion = servidor.expectOne('/api/admin/valoraciones/8/oculta');
    expect(peticion.request.body).toEqual({ oculta: true });
    peticion.flush(valoracionDePrueba({ id: 8, usuario: 'spam', nota: 1, oculta: true }));
    servidor.expectOne('/api/recetas/1').flush(detalleDePrueba({ nota: 5, valoraciones: 1 }));
    await fixture.whenStable();

    expect(html.querySelector('li.valoracion')!.textContent).toContain(
      'Oculta por la administración',
    );
    expect(html.querySelector('li.valoracion button')!.textContent!.trim()).toBe('Mostrar');
    expect(cambios).toEqual([{ nota: 5, valoraciones: 1, mia: null }]);
    expect(TestBed.inject(Notificaciones).avisos().at(-1)?.texto).toContain('ya no cuenta');
  });

  it('sin rol de administrador no hay botones de moderación', async () => {
    const html = await crear(detalleDePrueba(), [valoracionDePrueba()]);
    expect(html.querySelector('li.valoracion button')).toBeNull();
  });

  it('si falla la carga deja reintentar', async () => {
    fixture = TestBed.createComponent(Valoraciones);
    fixture.componentRef.setInput('receta', detalleDePrueba());
    await fixture.whenStable();
    lista().flush(null, { status: 500, statusText: 'Error' });
    await fixture.whenStable();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.alert-danger')!.textContent).toContain('No se han podido cargar');

    boton(html, 'Reintentar').click();
    lista().flush(paginaDe([valoracionDePrueba()]));
    await fixture.whenStable();
    expect(html.querySelectorAll('li.valoracion').length).toBe(1);
  });

  it('un cambio de la receta que no es otra receta no recarga ni borra lo escrito', async () => {
    const html = await crear();
    const comentario = html.querySelector<HTMLTextAreaElement>('#valoracion-comentario')!;
    comentario.value = 'A medias';
    comentario.dispatchEvent(new Event('input'));

    fixture.componentRef.setInput('receta', detalleDePrueba({ likes: 4, meGusta: true }));
    await fixture.whenStable();

    servidor.expectNone((r) => r.url === '/api/recetas/1/valoraciones');
    expect(comentario.value).toBe('A medias');
  });
});
