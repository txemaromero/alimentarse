import { Injectable } from '@angular/core';
import { CLAVE_MARCAS_COMPRA } from '../core/datos-sin-conexion';

/** Días que dura una marca: lo que se compró hace más de una semana vuelve a estar por comprar. */
export const DIAS_MARCA = 7;

const MS_POR_DIA = 24 * 60 * 60 * 1000;

/** Cuándo se marcó cada ingrediente (ms desde 1970), por id. */
type Marcas = Record<string, number>;

/**
 * Lo marcado en la lista de la compra, solo en este navegador (D-076): basta para ir a la compra con el móvil. Va por
 * ingrediente y no por semana, porque la semana del planificador empieza hoy y cambia cada día. Se borra al iniciar y al
 * cerrar sesión (`borrarDatosGuardados`).
 */
@Injectable({ providedIn: 'root' })
export class MarcasCompra {
  /** Los ingredientes marcados en los últimos {@link DIAS_MARCA} días. */
  leer(ahora = Date.now()): Set<number> {
    return new Set(Object.keys(vigentes(ahora)).map(Number).filter(Number.isInteger));
  }

  marcar(ingrediente: number, marcado: boolean, ahora = Date.now()): void {
    const marcas = vigentes(ahora);
    if (marcado) {
      marcas[ingrediente] = ahora;
    } else {
      delete marcas[ingrediente];
    }
    escribir(marcas);
  }

  desmarcar(ingredientes: Iterable<number>, ahora = Date.now()): void {
    const marcas = vigentes(ahora);
    for (const ingrediente of ingredientes) {
      delete marcas[ingrediente];
    }
    escribir(marcas);
  }
}

/** Las marcas guardadas que no han caducado; nada si no se pueden leer. */
function vigentes(ahora: number): Marcas {
  try {
    const datos: unknown = JSON.parse(localStorage.getItem(CLAVE_MARCAS_COMPRA) ?? '{}');
    const marcas: Marcas = {};
    if (datos && typeof datos === 'object' && !Array.isArray(datos)) {
      for (const [ingrediente, cuando] of Object.entries(datos)) {
        if (typeof cuando === 'number' && ahora - cuando < DIAS_MARCA * MS_POR_DIA) {
          marcas[ingrediente] = cuando;
        }
      }
    }
    return marcas;
  } catch {
    return {};
  }
}

function escribir(marcas: Marcas): void {
  try {
    if (Object.keys(marcas).length === 0) {
      localStorage.removeItem(CLAVE_MARCAS_COMPRA);
    } else {
      localStorage.setItem(CLAVE_MARCAS_COMPRA, JSON.stringify(marcas));
    }
  } catch {
    // Sin almacenamiento (navegación privada, cuota llena): las marcas duran lo que dure la pantalla
  }
}
