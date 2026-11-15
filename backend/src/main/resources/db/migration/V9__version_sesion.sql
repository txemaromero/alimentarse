-- Anular las sesiones abiertas al cambiar la contraseña o el rol (D-070): cada token lleva la versión con la que se
-- emitió y deja de valer si la de la cuenta ha subido.
ALTER TABLE usuarios ADD COLUMN VERSION_SESION INT NOT NULL DEFAULT 0;
