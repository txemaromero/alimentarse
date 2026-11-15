package es.alimentarse.alimentacion;

/**
 * Porcentaje (redondeado) de la ingesta de referencia de un adulto medio que supone un valor diario: 8.400 kJ
 * (2.000 kcal), 70 g de grasas, 20 g de saturadas, 260 g de hidratos, 50 g de proteínas y 6 g de sal (Reglamento (UE)
 * n.º 1169/2011, anexo XIII). La fibra no tiene ingesta de referencia en el reglamento (D-082).
 */
public record IngestaReferenciaDto(
        int energia, int grasas, int grasasSaturadas, int hidratos, int proteinas, int sal) {}
