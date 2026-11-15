import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Subject, of, throwError } from 'rxjs';
import { ModalConfirmacion, OpcionesConfirmacion } from './modal-confirmacion';

describe('ModalConfirmacion', () => {
  const activo = { close: vi.fn(), dismiss: vi.fn() };

  beforeEach(() => {
    activo.close.mockReset();
    activo.dismiss.mockReset();
    TestBed.configureTestingModule({ providers: [{ provide: NgbActiveModal, useValue: activo }] });
  });

  async function crear(opciones: Partial<OpcionesConfirmacion<unknown>> = {}) {
    const fixture = TestBed.createComponent(ModalConfirmacion);
    fixture.componentInstance.opciones = {
      titulo: 'Eliminar la receta',
      mensaje: '¿Seguro?',
      aceptar: 'Sí, eliminar',
      accion: () => of('hecho'),
      ...opciones,
    };
    await fixture.whenStable();
    return fixture;
  }

  function botonAceptar(html: HTMLElement): HTMLButtonElement {
    return html.querySelector('.btn-danger') as HTMLButtonElement;
  }

  it('muestra la pregunta y cierra con el resultado de la acción', async () => {
    const fixture = await crear();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('h2')?.textContent).toContain('Eliminar la receta');
    expect(html.textContent).toContain('¿Seguro?');

    botonAceptar(html).click();

    expect(activo.close).toHaveBeenCalledWith({ resultado: 'hecho' });
  });

  it('mientras la acción está en marcha no deja repetirla', async () => {
    const respuesta = new Subject<void>();
    const accion = vi.fn(() => respuesta);
    const fixture = await crear({ accion });
    const html = fixture.nativeElement as HTMLElement;

    botonAceptar(html).click();
    await fixture.whenStable();
    expect(botonAceptar(html).disabled).toBe(true);
    fixture.componentInstance.aceptar();

    expect(accion).toHaveBeenCalledTimes(1);
  });

  it('un error 4xx se muestra dentro del modal, sin cerrarlo', async () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { codigo: 'GRUPO_EN_USO', mensaje: 'El grupo tiene ingredientes.', errores: {} },
    });
    const fixture = await crear({ accion: () => throwError(() => error) });
    const html = fixture.nativeElement as HTMLElement;

    botonAceptar(html).click();
    await fixture.whenStable();

    expect(html.querySelector('.alert-danger')?.textContent).toContain(
      'El grupo tiene ingredientes.',
    );
    expect(activo.close).not.toHaveBeenCalled();
    expect(botonAceptar(html).disabled).toBe(false);
  });
});
