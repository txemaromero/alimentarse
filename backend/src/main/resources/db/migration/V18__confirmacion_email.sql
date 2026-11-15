-- Confirmación del email (D-088). EMAIL_VERIFICADO: su dueño ha abierto el enlace que se le envió (o lo ha verificado
-- Google). Un email nuevo espera en EMAIL_PENDIENTE hasta que se confirma; mientras, la cuenta sigue con el anterior.
-- Del token del enlace, como del de recuperar la contraseña, solo se guarda el SHA-256.
-- Las cuentas que ya existen empiezan sin confirmar: no se sabe si su email es suyo.
ALTER TABLE usuarios
    ADD COLUMN EMAIL_VERIFICADO       BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN EMAIL_PENDIENTE        VARCHAR(100) NULL,
    ADD COLUMN TOKEN_EMAIL            VARCHAR(100) NULL,
    ADD COLUMN TOKEN_EMAIL_EXPIRACION TIMESTAMP    NULL,
    ADD CONSTRAINT UK_USUARIOS_TOKEN_EMAIL UNIQUE (TOKEN_EMAIL);
