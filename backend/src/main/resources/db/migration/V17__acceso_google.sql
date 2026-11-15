-- Acceso con Google (D-085): el identificador de la cuenta de Google («sub» de OpenID Connect) con la que el usuario
-- puede entrar, o NULL. Una cuenta de Google solo puede estar vinculada a un usuario.
ALTER TABLE usuarios
    ADD COLUMN GOOGLE_ID VARCHAR(255) NULL,
    ADD CONSTRAINT UK_USUARIOS_GOOGLE_ID UNIQUE (GOOGLE_ID);
