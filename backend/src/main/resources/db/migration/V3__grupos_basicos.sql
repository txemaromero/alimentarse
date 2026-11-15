-- Grupos de ingredientes básicos (sal, aceite, agua, especias…): están en casi todas las recetas, así que
-- «Ingredientes más usados» de «Mi alimentación» no los muestra (D-054). No cambia ninguna regla de las raciones.
ALTER TABLE grupo_ingrediente
    ADD COLUMN BASICO BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE grupo_ingrediente
SET BASICO = TRUE
WHERE NOMBRE IN ('Esenciales', 'Especias');
