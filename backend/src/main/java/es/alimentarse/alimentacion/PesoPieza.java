package es.alimentarse.alimentacion;

import java.math.BigDecimal;

/**
 * Peso aproximado de una pieza de un ingrediente («unidad», «diente», «loncha»), en gramos de porción comestible
 * (D-079). Es una estimación propia, no un dato de BEDCA.
 *
 * @param pieza en singular y en minúsculas
 */
record PesoPieza(int ingrediente, String pieza, BigDecimal gramos) {}
