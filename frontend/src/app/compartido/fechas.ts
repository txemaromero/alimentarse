import { ValidatorFn } from '@angular/forms';

/** Margen para no rechazar «ahora» por pequeñas diferencias de reloj con el servidor (que admite 1 minuto). */
const MARGEN_MS = 60_000;

/** Fecha y hora locales en el formato de `<input type="datetime-local">` (`yyyy-MM-ddTHH:mm`). */
export function aValorLocal(fecha: Date): string {
  const dos = (n: number) => String(n).padStart(2, '0');
  return (
    `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}` +
    `T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`
  );
}

export function ahoraLocal(): string {
  return aValorLocal(new Date());
}

/** Convierte el valor de un `datetime-local` (hora del navegador) a un instante ISO-8601 en UTC. */
export function aInstanteUtc(valorLocal: string): string {
  return new Date(valorLocal).toISOString();
}

/** El valor de un `datetime-local` no puede ser posterior a ahora (6.7). */
export const noFutura: ValidatorFn = (control) => {
  const valor = control.value as string | null;
  if (!valor) {
    return null;
  }
  const instante = new Date(valor).getTime();
  if (Number.isNaN(instante)) {
    return { fechaInvalida: true };
  }
  return instante > Date.now() + MARGEN_MS ? { futura: true } : null;
};
