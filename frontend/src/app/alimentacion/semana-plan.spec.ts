import {
  MOMENTOS,
  SEMANAS_MAXIMAS,
  claveHueco,
  diasDeLaSemana,
  finDelDia,
  instanteDe,
  momentoDe,
  semanaDeLaUrl,
} from './semana-plan';

describe('semana del planificador', () => {
  const hoy = new Date(2026, 10, 28, 17, 45);

  it('empieza hoy, a las 00:00, y tiene 7 días', () => {
    const dias = diasDeLaSemana(hoy, 0);
    expect(dias).toHaveLength(7);
    expect(dias[0]).toEqual(new Date(2026, 10, 28));
    // Cambia de mes sin problemas
    expect(dias[6]).toEqual(new Date(2026, 11, 4));
  });

  it('las semanas siguientes avanzan de 7 en 7', () => {
    expect(diasDeLaSemana(hoy, 2)[0]).toEqual(new Date(2026, 11, 12));
  });

  it('se puede adelantar sin salirse de los 90 días que admite el servidor', () => {
    const ultimo = diasDeLaSemana(hoy, SEMANAS_MAXIMAS)[6];
    const dias = (ultimo.getTime() - hoy.getTime()) / 86_400_000;
    expect(dias).toBeLessThanOrEqual(90);
  });

  it('la semana de la URL queda entre la actual y la última que se puede planificar', () => {
    expect(semanaDeLaUrl('3')).toBe(3);
    expect(semanaDeLaUrl(2)).toBe(2);
    expect(semanaDeLaUrl('2.7')).toBe(2);
    expect(semanaDeLaUrl(undefined)).toBe(0);
    expect(semanaDeLaUrl('abc')).toBe(0);
    expect(semanaDeLaUrl('-4')).toBe(0);
    expect(semanaDeLaUrl('99')).toBe(SEMANAS_MAXIMAS);
  });

  it('el final del día es su último segundo', () => {
    expect(finDelDia(new Date(2026, 10, 28))).toEqual(new Date(2026, 10, 28, 23, 59, 59));
  });

  it('cada momento tiene su hora local', () => {
    const dia = new Date(2026, 10, 28);
    expect(instanteDe(dia, MOMENTOS[0])).toEqual(new Date(2026, 10, 28, 14));
    expect(instanteDe(dia, MOMENTOS[1])).toEqual(new Date(2026, 10, 28, 21));
  });

  it('sabe a qué hueco pertenece una comida', () => {
    expect(momentoDe(new Date(2026, 10, 28, 14))).toBe('comida');
    expect(momentoDe(new Date(2026, 10, 28, 17, 59))).toBe('comida');
    expect(momentoDe(new Date(2026, 10, 28, 21))).toBe('cena');
  });

  it('la clave identifica el día y el momento', () => {
    expect(claveHueco(new Date(2026, 10, 28), 'cena')).toBe('2026-10-28-cena');
  });
});
