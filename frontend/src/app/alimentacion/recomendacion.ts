import { ImpactoGrupo, IndicadorGrupo } from '../core/modelos';

/** «A», «A y B», «A, B y C». */
function enumerar(textos: string[]): string {
  return textos.length < 2
    ? textos.join('')
    : `${textos.slice(0, -1).join(', ')} y ${textos[textos.length - 1]}`;
}

function nombres(grupos: IndicadorGrupo[]): string {
  return enumerar(grupos.map((g) => g.grupo.nombre));
}

/**
 * Por qué se recomienda una receta (D-067), a partir de cómo van esta semana sus grupos de alimentos. `null` si no hay
 * nada que explicar (solo lleva ingredientes básicos, como sal o aceite).
 */
export function motivoRecomendacion(grupos: IndicadorGrupo[]): string | null {
  if (grupos.length === 0) {
    return null;
  }
  const conLimite = grupos.filter((g) => g.limite !== null);
  if (conLimite.length === 0) {
    return `Sin límite semanal: ${nombres(grupos)}.`;
  }
  const empezados = conLimite.filter((g) => g.consumidas > 0);
  if (empezados.length === 0) {
    return `Sin consumir esta semana: ${nombres(conLimite)}.`;
  }
  const raciones = empezados.map((g) => `${g.grupo.nombre} (${g.consumidas}/${g.limite})`);
  return `Te quedan raciones de ${enumerar(raciones)}.`;
}

/** ¿Qué pasaría con el límite semanal del grupo si comiera la receta? */
export function avisoImpacto(impacto: ImpactoGrupo): 'llega' | 'supera' | null {
  const { consumidas, limite } = impacto.despues;
  if (limite === null || consumidas < limite) {
    return null;
  }
  return consumidas === limite ? 'llega' : 'supera';
}
