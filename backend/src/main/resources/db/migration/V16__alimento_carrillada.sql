-- Alimento de BEDCA para la Carrillada (D-084). BEDCA no publica el detalle de la carrillera de vacuno (2710, ver V14),
-- así que se usa el corte de cerdo más parecido: la cabezada, con su parte grasa y magra, cruda (2700). La carrillada
-- suele ser de cerdo y sus valores son parecidos. Si la administración ya le había asignado otro, se respeta.
-- Levadura sigue sin alimento: BEDCA no tiene levadura.
INSERT INTO ingrediente_alimento (INGREDIENTE, ALIMENTO)
SELECT i.ID, 2700
FROM ingredientes i
WHERE i.NOMBRE = 'Carrillada'
  AND NOT EXISTS (SELECT 1 FROM ingrediente_alimento ia WHERE ia.INGREDIENTE = i.ID);

-- Peso aproximado de una carrillada de cerdo, en gramos (estimación propia, como las piezas de V13)
INSERT INTO peso_pieza_ingrediente (INGREDIENTE, PIEZA, GRAMOS)
SELECT i.ID, 'unidad', 120
FROM ingredientes i
WHERE i.NOMBRE = 'Carrillada'
  AND NOT EXISTS (SELECT 1 FROM peso_pieza_ingrediente p WHERE p.INGREDIENTE = i.ID AND p.PIEZA = 'unidad');
