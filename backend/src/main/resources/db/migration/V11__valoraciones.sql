-- Valoraciones de las recetas (D-077): una nota de 1 a 5 y un comentario opcional por usuario y receta, que su autor
-- puede cambiar o borrar. Conviven con los «me gusta», que son los favoritos de cada uno. La administración puede
-- ocultar una valoración: deja de verse y de contar en la nota media.
CREATE TABLE valoracion_receta (
    ID         BIGINT        NOT NULL AUTO_INCREMENT,
    USUARIO    VARCHAR(100)  NOT NULL,
    RECETA     INT           NOT NULL,
    NOTA       TINYINT       NOT NULL,
    -- Texto plano, sin HTML
    COMENTARIO VARCHAR(1000) NULL,
    -- La de la última vez que se guardó
    FECHA      TIMESTAMP     NOT NULL,
    OCULTA     BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT PK_VALORACION_RECETA PRIMARY KEY (ID),
    CONSTRAINT UK_VALORACION_RECETA_USUARIO_RECETA UNIQUE (USUARIO, RECETA),
    CONSTRAINT FK_VALORACION_RECETA_USUARIO FOREIGN KEY (USUARIO) REFERENCES usuarios (ID),
    CONSTRAINT FK_VALORACION_RECETA_RECETA FOREIGN KEY (RECETA) REFERENCES recetas (ID) ON DELETE CASCADE,
    CONSTRAINT CK_VALORACION_RECETA_NOTA CHECK (NOTA BETWEEN 1 AND 5),
    INDEX IX_VALORACION_RECETA_RECETA_FECHA (RECETA, FECHA)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
