-- Recetas eliminadas por su autor que alguien ya había consumido (D-061). Se conservan el título y los ingredientes
-- para que esos consumos sigan contando en las raciones de los últimos 7 días y en el histórico, pero ya nadie las ve
-- ni las puede abrir. Las que nadie ha consumido se borran de verdad.
ALTER TABLE recetas
    ADD COLUMN ELIMINADA BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT CK_RECETAS_ELIMINADA CHECK (NOT (ELIMINADA AND PUBLICADA));
