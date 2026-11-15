-- Galería de fotos de las recetas (D-083): hasta 6 por receta, en orden, con una descripción opcional (el texto
-- alternativo). La primera (ORDEN 0) es la principal y se copia en recetas.IMAGEN, que siguen usando las tarjetas y
-- los listados.
CREATE TABLE foto_receta (
    ID          INT          NOT NULL AUTO_INCREMENT,
    RECETA      INT          NOT NULL,
    -- Nombre del fichero dentro de UPLOAD_DIR (o de una imagen de demostración)
    NOMBRE      VARCHAR(100) NOT NULL,
    -- 0 = la principal
    ORDEN       INT          NOT NULL,
    DESCRIPCION VARCHAR(150) NULL,
    CONSTRAINT PK_FOTO_RECETA PRIMARY KEY (ID),
    CONSTRAINT UK_FOTO_RECETA_ORDEN UNIQUE (RECETA, ORDEN),
    CONSTRAINT UK_FOTO_RECETA_NOMBRE UNIQUE (NOMBRE),
    CONSTRAINT FK_FOTO_RECETA_RECETA FOREIGN KEY (RECETA) REFERENCES recetas (ID) ON DELETE CASCADE,
    CONSTRAINT CK_FOTO_RECETA_ORDEN CHECK (ORDEN BETWEEN 0 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- La imagen que ya tenía cada receta pasa a ser la primera foto de su galería
INSERT INTO foto_receta (RECETA, NOMBRE, ORDEN)
SELECT ID, IMAGEN, 0 FROM recetas WHERE IMAGEN IS NOT NULL;

-- Enlace opcional a un vídeo de la receta en YouTube o Vimeo (D-083): un enlace, no un vídeo incrustado
ALTER TABLE recetas ADD COLUMN VIDEO VARCHAR(500) NULL;
