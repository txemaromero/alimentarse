import { ImpactoGrupo, IndicadorGrupo } from '../core/modelos';
import { avisoImpacto, motivoRecomendacion } from './recomendacion';

function grupo(nombre: string, consumidas: number, limite: number | null): IndicadorGrupo {
  return {
    grupo: { id: nombre.length, nombre, racionesSemana: limite },
    consumidas,
    limite,
    porcentaje: 0,
    color: 'VERDE',
  };
}

describe('motivoRecomendacion', () => {
  it('sin grupos que explicar no dice nada', () => {
    expect(motivoRecomendacion([])).toBeNull();
  });

  it('nombra los grupos sin límite si no hay otros', () => {
    expect(motivoRecomendacion([grupo('Fruta', 3, null), grupo('Verduras', 0, null)])).toBe(
      'Sin límite semanal: Fruta y Verduras.',
    );
  });

  it('destaca los grupos con límite que aún no se han comido', () => {
    expect(motivoRecomendacion([grupo('Legumbres', 0, 4), grupo('Hortalizas', 5, null)])).toBe(
      'Sin consumir esta semana: Legumbres.',
    );
  });

  it('enumera los grupos con límite ya empezados y sus raciones', () => {
    expect(
      motivoRecomendacion([grupo('Carne', 2, 3), grupo('Pasta', 1, 4), grupo('Arroz', 0, 4)]),
    ).toBe('Te quedan raciones de Carne (2/3) y Pasta (1/4).');
  });

  it('separa con comas cuando son más de dos', () => {
    expect(
      motivoRecomendacion([grupo('Carne', 1, 3), grupo('Pasta', 1, 4), grupo('Arroz', 1, 4)]),
    ).toBe('Te quedan raciones de Carne (1/3), Pasta (1/4) y Arroz (1/4).');
  });
});

describe('avisoImpacto', () => {
  const impacto = (antes: number, limite: number): ImpactoGrupo => ({
    antes: grupo('Carne', antes, limite),
    despues: grupo('Carne', antes + 1, limite),
  });

  it('no avisa si aún queda margen', () => {
    expect(avisoImpacto(impacto(0, 3))).toBeNull();
  });

  it('avisa al llegar justo al límite', () => {
    expect(avisoImpacto(impacto(2, 3))).toBe('llega');
  });

  it('avisa más fuerte al pasarse', () => {
    expect(avisoImpacto(impacto(3, 3))).toBe('supera');
  });
});
