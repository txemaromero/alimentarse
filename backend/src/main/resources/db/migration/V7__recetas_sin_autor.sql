-- Baja de una cuenta (D-064). Sus recetas que otros ya han consumido se conservan, eliminadas (D-061) y sin autor,
-- para que esos consumos sigan contando en las raciones de la semana. Solo una receta eliminada puede quedarse sin
-- autor.
ALTER TABLE recetas
    MODIFY USUARIO VARCHAR(100) NULL,
    ADD CONSTRAINT CK_RECETAS_AUTOR CHECK (USUARIO IS NOT NULL OR ELIMINADA);
