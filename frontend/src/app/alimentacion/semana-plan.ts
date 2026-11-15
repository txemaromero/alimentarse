/** Cálculos de fechas del planificador, siempre en la hora local del navegador (D-068). */

export type ClaveMomento = 'comida' | 'cena';

export interface Momento {
  clave: ClaveMomento;
  etiqueta: string;
  /** Hora local con la que se guarda la comida. */
  hora: number;
}

export const MOMENTOS: readonly Momento[] = [
  { clave: 'comida', etiqueta: 'Comida', hora: 14 },
  { clave: 'cena', etiqueta: 'Cena', hora: 21 },
];

const DIAS_POR_SEMANA = 7;
/** Semanas que se pueden adelantar: se queda dentro de los 90 días que admite el servidor. */
export const SEMANAS_MAXIMAS = 11;

/**
 * La semana que pide la URL (`?semana=2`) entre 0 y {@link SEMANAS_MAXIMAS}; 0 si falta o no es un número. La comparten
 * el planificador y la lista de la compra (D-076).
 */
export function semanaDeLaUrl(valor: unknown): number {
  const semana = Math.trunc(Number(valor));
  return Number.isFinite(semana) ? Math.min(Math.max(semana, 0), SEMANAS_MAXIMAS) : 0;
}

/** Los 7 días de la semana `semana` (0 = la que empieza hoy), a las 00:00 locales. */
export function diasDeLaSemana(hoy: Date, semana: number): Date[] {
  const inicio = hoy.getDate() + semana * DIAS_POR_SEMANA;
  return Array.from(
    { length: DIAS_POR_SEMANA },
    (_, i) => new Date(hoy.getFullYear(), hoy.getMonth(), inicio + i),
  );
}

/** Último segundo de un día: el final de la semana que se muestra. */
export function finDelDia(dia: Date): Date {
  return new Date(dia.getFullYear(), dia.getMonth(), dia.getDate(), 23, 59, 59);
}

/** Momento exacto con el que se guarda una comida: construido en hora local, resiste los cambios de hora. */
export function instanteDe(dia: Date, momento: Momento): Date {
  return new Date(dia.getFullYear(), dia.getMonth(), dia.getDate(), momento.hora);
}

/** A qué hueco pertenece una comida; por tramos, por si se creó en otra zona horaria. */
export function momentoDe(fecha: Date): ClaveMomento {
  return fecha.getHours() < 18 ? 'comida' : 'cena';
}

export function claveHueco(dia: Date, momento: ClaveMomento): string {
  return `${dia.getFullYear()}-${dia.getMonth()}-${dia.getDate()}-${momento}`;
}
