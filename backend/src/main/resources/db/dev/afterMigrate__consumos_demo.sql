-- Callback de Flyway (solo perfil dev): se ejecuta tras cada migrate, es decir, en cada arranque.
-- Regenera el historial de consumos de las cuentas demo con fechas relativas a "ahora" (D-015),
-- para que "Mi alimentación" y "¿Qué toca hoy?" siempre muestren datos.
--
-- lucia, últimos 7 días (reproduce el ejemplo de la sección 6.4):
--   Pollo con tallarines x3  -> Carne 3/3 (rojo), Pasta 3/4 (amarillo)
--   Boquerones fritos x1     -> Pescado 1/4 (verde)
--   Batido x2 + limón de los boquerones -> Fruta 3/∞ (verde)
--   Rollos de canela hace 9 días: fuera de la ventana, Dulces no cuenta.
-- marcos: rollos de canela hace 2 días -> Dulces 1/1 agotado (6.3 excluye las recetas con dulces).

DELETE FROM usuario_receta_consumida WHERE USUARIO IN ('lucia', 'marcos', 'carmen');

INSERT INTO usuario_receta_consumida (USUARIO, RECETA, FECHA)
SELECT v.usuario, v.receta, NOW() - INTERVAL v.horas HOUR
FROM (VALUES
    ROW('lucia', 10, 10),
    ROW('lucia', 6, 26),
    ROW('lucia', 3, 50),
    ROW('lucia', 6, 74),
    ROW('lucia', 10, 98),
    ROW('lucia', 6, 122),
    ROW('lucia', 5, 218),

    ROW('marcos', 8, 21),
    ROW('marcos', 5, 49),
    ROW('marcos', 4, 75),
    ROW('marcos', 1, 196),

    ROW('carmen', 10, 5),
    ROW('carmen', 1, 145),
    ROW('carmen', 9, 194),
    ROW('carmen', 7, 230)
) AS v (usuario, receta, horas);
