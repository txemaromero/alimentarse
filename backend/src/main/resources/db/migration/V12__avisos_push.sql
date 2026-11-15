-- Avisos push (D-078): los navegadores que reciben el recordatorio de las comidas planificadas.
CREATE TABLE suscripcion_push (
    ID            BIGINT        NOT NULL AUTO_INCREMENT,
    USUARIO       VARCHAR(100)  NOT NULL,
    -- URL del servicio push de ese navegador; quien la conoce puede enviarle avisos
    ENDPOINT      VARCHAR(1000) NOT NULL,
    -- SHA-256 del endpoint en hexadecimal: un índice único sobre 1000 caracteres pasaría del límite de InnoDB
    ENDPOINT_HASH CHAR(64)      NOT NULL,
    -- Clave pública y secreto del navegador para cifrar los avisos (RFC 8291), en Base64 URL
    P256DH        VARCHAR(100)  NOT NULL,
    AUTH          VARCHAR(50)   NOT NULL,
    -- Zona horaria del navegador, para escribir la hora de la comida en el aviso
    ZONA          VARCHAR(50)   NOT NULL,
    FECHA         TIMESTAMP     NOT NULL,
    CONSTRAINT PK_SUSCRIPCION_PUSH PRIMARY KEY (ID),
    CONSTRAINT UK_SUSCRIPCION_PUSH_ENDPOINT_HASH UNIQUE (ENDPOINT_HASH),
    CONSTRAINT FK_SUSCRIPCION_PUSH_USUARIO FOREIGN KEY (USUARIO) REFERENCES usuarios (ID)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Cuándo se envió el recordatorio de cada comida planificada: así se envía una sola vez, aunque haya varios servidores
ALTER TABLE usuario_receta_planificada
    ADD COLUMN AVISADA TIMESTAMP NULL,
    ADD INDEX IX_USUARIO_RECETA_PLANIFICADA_FECHA (FECHA);
