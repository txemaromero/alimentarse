-- Moderación (D-063). Una receta oculta por un administrador solo la ven su autor y los administradores; el motivo se
-- le muestra al autor. Un usuario bloqueado no puede iniciar sesión y su sesión abierta deja de valer.
ALTER TABLE recetas
    ADD COLUMN OCULTA        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN MOTIVO_OCULTA VARCHAR(500) NULL;

ALTER TABLE usuarios
    ADD COLUMN BLOQUEADO BOOLEAN NOT NULL DEFAULT FALSE;
