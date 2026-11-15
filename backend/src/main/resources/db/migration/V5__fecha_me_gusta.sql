-- Fecha de cada «me gusta», para ordenar «Recetas que me gustan» de la más reciente a la más antigua (D-062).
-- Los que ya existían toman la fecha de esta migración.
ALTER TABLE like_receta
    ADD COLUMN FECHA TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
