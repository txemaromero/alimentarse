import { TestBed } from '@angular/core/testing';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { OpcionesConfirmacion } from '../app/compartido/modal-confirmacion';

/** `NgbModal.open` simulado para `confirmar()`: acepta la confirmación ejecutando su acción, como haría el modal. */
export function aceptarConfirmacion() {
  const instancia: { opciones?: OpcionesConfirmacion<unknown> } = {};
  let resolver!: (valor: unknown) => void;
  const resultado = new Promise((r) => (resolver = r));
  vi.spyOn(TestBed.inject(NgbModal), 'open').mockReturnValue({
    componentInstance: instancia,
    result: resultado,
  } as never);
  return {
    instancia,
    /** Lanza la acción (la petición queda pendiente para el test) y cierra el modal cuando responde. */
    aceptar: () => instancia.opciones!.accion().subscribe((r) => resolver({ resultado: r })),
  };
}
