-- Límite de intentos compartido entre servidores (D-071): una ventana por clave (por ejemplo, login:<ip>).
CREATE TABLE limite_intentos (
    CLAVE    VARCHAR(100) NOT NULL,
    FIN      TIMESTAMP    NOT NULL,
    INTENTOS INT          NOT NULL,
    CONSTRAINT PK_LIMITE_INTENTOS PRIMARY KEY (CLAVE),
    INDEX IX_LIMITE_INTENTOS_FIN (FIN)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
