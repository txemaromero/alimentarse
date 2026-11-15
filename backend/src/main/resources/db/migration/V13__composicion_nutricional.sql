-- Estudio nutricional (D-079): composición de los ingredientes del catálogo y peso aproximado de sus piezas.
--
-- Fuente de la composición: AESAN/BEDCA Base de Datos Española de Composición de Alimentos v1.0 (2010),
-- https://www.bedca.net/. Uso educativo y no comercial, citando la fuente. Los valores son los de BEDCA sin cambios,
-- por 100 g de porción comestible; NULL donde BEDCA no da el dato. Única conversión: la energía de la pimienta, que
-- BEDCA da en kcal (230), se guarda en kJ (x 4,184). Para cada ingrediente se ha elegido el alimento de BEDCA más
-- genérico (crudo o fresco si lo hay); su nombre se guarda para mostrarlo. Sin alimento adecuado en BEDCA:
-- Levadura, Carrillada.

CREATE TABLE composicion_ingrediente (
    INGREDIENTE      INT           NOT NULL,
    BEDCA_ID         INT           NOT NULL,
    BEDCA_NOMBRE     VARCHAR(150)  NOT NULL,
    -- Por 100 g de porción comestible: energía en kJ, sodio en mg y el resto en g
    ENERGIA_KJ       DECIMAL(12,6) NULL,
    GRASAS           DECIMAL(12,6) NULL,
    GRASAS_SATURADAS DECIMAL(12,6) NULL,
    HIDRATOS         DECIMAL(12,6) NULL,
    FIBRA            DECIMAL(12,6) NULL,
    PROTEINAS        DECIMAL(12,6) NULL,
    SODIO_MG         DECIMAL(12,6) NULL,
    CONSTRAINT PK_COMPOSICION_INGREDIENTE PRIMARY KEY (INGREDIENTE),
    CONSTRAINT FK_COMPOSICION_INGREDIENTE_INGREDIENTE FOREIGN KEY (INGREDIENTE) REFERENCES ingredientes (ID)
        ON DELETE CASCADE,
    CONSTRAINT CK_COMPOSICION_INGREDIENTE_VALORES CHECK (
        (ENERGIA_KJ IS NULL OR ENERGIA_KJ >= 0) AND (GRASAS IS NULL OR GRASAS >= 0)
        AND (GRASAS_SATURADAS IS NULL OR GRASAS_SATURADAS >= 0) AND (HIDRATOS IS NULL OR HIDRATOS >= 0)
        AND (FIBRA IS NULL OR FIBRA >= 0) AND (PROTEINAS IS NULL OR PROTEINAS >= 0)
        AND (SODIO_MG IS NULL OR SODIO_MG >= 0))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Peso aproximado de una pieza («unidad», «diente», «loncha»), en gramos de porción comestible: estimación propia,
-- no de BEDCA, para convertir cantidades como «2 unidades» o «3 dientes».
CREATE TABLE peso_pieza_ingrediente (
    INGREDIENTE INT          NOT NULL,
    -- En singular y en minúsculas
    PIEZA       VARCHAR(30)  NOT NULL,
    GRAMOS      DECIMAL(7,1) NOT NULL,
    CONSTRAINT PK_PESO_PIEZA_INGREDIENTE PRIMARY KEY (INGREDIENTE, PIEZA),
    CONSTRAINT FK_PESO_PIEZA_INGREDIENTE_INGREDIENTE FOREIGN KEY (INGREDIENTE) REFERENCES ingredientes (ID)
        ON DELETE CASCADE,
    CONSTRAINT CK_PESO_PIEZA_INGREDIENTE_GRAMOS CHECK (GRAMOS > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Por nombre: si la administración ha renombrado o fusionado un ingrediente, ese simplemente no se rellena
INSERT INTO composicion_ingrediente
    (INGREDIENTE, BEDCA_ID, BEDCA_NOMBRE, ENERGIA_KJ, GRASAS, GRASAS_SATURADAS, HIDRATOS, FIBRA, PROTEINAS, SODIO_MG)
SELECT i.ID, v.bedca_id, v.bedca_nombre, v.energia_kj, v.grasas, v.grasas_saturadas, v.hidratos, v.fibra,
       v.proteinas, v.sodio_mg
FROM (VALUES
    ROW('Aceite de oliva', 2543, 'Aceite de oliva', 3696.3, 99.9, 17.06, 0, 0, 0, 0),
    ROW('Aceite de girasol', 2541, 'Aceite de girasol', 3696.3, 99.9, 10.62, 0, 0, NULL, 0),
    ROW('Sal', 866, 'Sal de mar', 0, NULL, NULL, 0, 0, 0, 35000),
    ROW('Agua', 2425, 'Agua de la red', 0, 0, 0, 0, 0, 0, NULL),
    ROW('Azúcar', 2642, 'Azúcar blanca', 1700, 0, 0, 100, 0, 0, 0),
    ROW('Azúcar moreno', 780, 'Azúcar, moreno', 1659.2, 0, 0, 97.6, 0, 0, 40),
    ROW('Harina', 2677, 'Harina de trigo', 1385.5, NULL, NULL, 71.5, 3.5, 10, 3),
    ROW('Vinagre', 2124, 'Vinagre de vino', 17, 0, 0, 0.6, 0, 0.4, 8),
    ROW('Maicena', 1012, 'Almidón de maíz', 1558.37, 0, NULL, 91.3, 0.9, 0.26, 9),
    ROW('Canela', 2477, 'Canela, en polvo', 1554.73, 3.2, 0.65, 80.59, 54.3, 3.9, 26.3),
    ROW('Pimienta', 2590, 'Pimienta, negra', 962.320, 3.3, 0.98, 38.3, 26.5, 11, 44),
    ROW('Pimentón', 2592, 'Pimentón, en polvo', 1378.6, 13, 2.14, 38, 20, 14.8, 34),
    ROW('Orégano', 2598, 'Orégano, seco', 1739.74, 10.3, 2.7, 68.92, 42.8, 11, 15),
    ROW('Comino', 2571, 'Comino', 1747.8, 18.2, NULL, 45.4, 10.5, 17.8, 168),
    ROW('Laurel', 2603, 'Laurel, hoja', 1266.2, 8.4, 2.28, 48.6, 26.3, 7.6, 23),
    ROW('Perejil', 2593, 'Perejil, fresco', 145, 1.3, NULL, 2.7, 5, 3, 33),
    ROW('Nuez moscada', 2599, 'Nuez moscada', 2279.63, 36.3, 25.9, 49.29, 20.8, 5.8, 16.2),
    ROW('Espárragos', 1179, 'Espárrago, verde', 105.5, 0.6, 0.1, 2, 1.7, 2.9, 3),
    ROW('Espinacas', 861, 'Espinaca, picada, congelada, cruda', 92.7125, 0.8, 0.24, 1, 2.9, 2.7125, 56),
    ROW('Lechuga', 2399, 'Lechuga', 65.125, 0.6, 0.12, 1.4, 1.5, 1.125, 3),
    ROW('Brócoli', 1177, 'Brécol, crudo', 106.6, 0.4, 0.07, 2.4, 3, 3, 13),
    ROW('Judías verdes', 2198, 'Judía verde, cruda', 115.1, 0.4, 0.09, 3.7, 2.8, 2.2, 4),
    ROW('Champiñones', 2384, 'Champiñon', 109.7, 0.3, 0.07, 4, 2.5, 1.8, 5),
    ROW('Patata', 2403, 'Patata, cruda', 303.2, 0.2, 0.03, 15.2, 1.7, 2.2, 15),
    ROW('Cebolla', 2381, 'Cebolla', 109.225, NULL, NULL, 5.3, 1.8, 1.125, 3),
    ROW('Ajo', 2361, 'Ajo', 488.547, 0.23, 0.05, 24.3, 1.2, 3.9375, 19),
    ROW('Calabaza', 2378, 'Calabaza, cruda', 134.1, 0.5, 0.26, 5.6, 2.4, 1.2, 1),
    ROW('Puerro', 2411, 'Puerro, crudo y congelado', 101.2, 0.3, NULL, 3.7, 2.8, 1.6, 12),
    ROW('Berenjena', 2370, 'Berenjena', 83.9, 0.2, NULL, 3.8, 2.4, 0.7, 3),
    ROW('Tomate', 2421, 'Tomate', 78.5, 0.1, NULL, 3.5, 1.1, 0.9, 18),
    ROW('Zanahoria', 2424, 'Zanahoria, cruda', 143.7, 0.3, 0.05, 7, 2.6, 0.8, 70),
    ROW('Pimiento', 2409, 'Pimiento rojo, crudo', 120.8, 0.6, 0.14, 4.5, 1.8, 1.3, 6),
    ROW('Calabacín', 2376, 'Calabacín', 72, 0.2, 0.04, 2, 1, 1.8, 3),
    ROW('Pepino', 2407, 'Pepino', 49.6, 0.1, NULL, 2, 0.8, 0.7, 3),
    ROW('Limón', 937, 'Zumo de limón, fresco', 71.7, 0.1, NULL, 3.6, 0.1, 0.4, 1),
    ROW('Manzana', 2231, 'Manzana', 209.1, NULL, 0.2, 12, 2, 0.3, 2),
    ROW('Plátano', 2245, 'Plátano', 371.5, 0.3, 0.11, 20, 3.4, 1.2, 1),
    ROW('Naranja', 2235, 'Naranja', 159.8, NULL, 0.028, 8.6, 2, 0.8, 3),
    ROW('Fresa', 2225, 'Fresa', 149.4, 0.5, 0.032, 7, 2.2, 0.7, 2),
    ROW('Pera', 2241, 'Pera', 187, NULL, 0.014, 10.6, 2.3, 0.4, 2),
    ROW('Lentejas', 1065, 'Lenteja, seca, cruda', 1292.26, 1.17286, 0.172121, 48.6954, 9.6975, 24.7674, 226.78),
    ROW('Garbanzos', 1020, 'Garbanzo, seco, crudo', 1398.47, 6.3, 0.4275, 49.2463, 14.9667, 19.305, 30),
    ROW('Alubias blancas', 2196, 'Judía blanca', 1007.8, 1.6, 0.53, 34.7, 23.2, 21.1, 15),
    ROW('Guisantes', 1175, 'Guisante, congelado, crudo', 259.9, 0.5, 0.1, 8.8, 4.7, 5.4, 3),
    ROW('Tallarines', 1117, 'Pasta alimenticia, cruda', 1471.37, 1.44776, 0.19, 70.9, 5, 12.5, 5),
    ROW('Macarrones', 1117, 'Pasta alimenticia, cruda', 1471.37, 1.44776, 0.19, 70.9, 5, 12.5, 5),
    ROW('Espaguetis', 1117, 'Pasta alimenticia, cruda', 1471.37, 1.44776, 0.19, 70.9, 5, 12.5, 5),
    ROW('Arroz', 2660, 'Arroz', 1614.3, 0.9, 0.21, 86, 0.2, 7, 6),
    ROW('Atún', 2134, 'Atún, crudo', 496.1, 3.3, 0.96, 0, 0, 22, 47),
    ROW('Boquerón', 2316, 'Boquerón', 532.3, 6.3, 1.65, 0, 0, 17.6, 116),
    ROW('Sardina', 2630, 'Sardina', 585.2, 7.5, 2.64, 0, 0, 18.1, 100),
    ROW('Merluza', 2347, 'Merluza fresca', 269.41, 1.8, 0.35, 0, 0, 11.93, 101),
    ROW('Salmón', 2626, 'Salmón', 756.8, 12, 2.16, 0, 0, 18.4, 98),
    ROW('Bacalao', 2302, 'Bacalao, crudo', 346.4, 1, 0.17, NULL, NULL, 18.2, 68),
    ROW('Huevos', 2127, 'Huevo de gallina fresco', 623.2, 11.1, 3.1, NULL, 0, 12.5, 140),
    ROW('Pollo', 2292, 'Pollo, entero, con piel, crudo', 696.7, 9.25, 2.66, 0, 0, 20.85, 63),
    ROW('Ternera', 2315, 'Ternera, parte sin especificar, cruda, con grasa separable', 1060.9, 21, 8.51, 0, 0, 16.7, 61),
    ROW('Lomo de cerdo', 2259, 'Cerdo, lomo, crudo', 635.3, 8.9, 3.28, 0, 0, 18, 63),
    ROW('Bacon', 682, 'Bacon, crudo, con grasa separable', 2276.93, 57.5, 21.26, 0.09, 0, 8.7, 680),
    ROW('Chorizo', 2264, 'Chorizo', 1346, 23.1, 9.6, 1.9, 0, 27, 1060),
    ROW('Jamón serrano', 2273, 'Jamón serrano', 1329.2, 22.6, 7.94, 0.2, 0, 28.8, 2130),
    ROW('Jamón cocido', 965, 'Jamón cocido, categoría s/e', 474.8, 3, 1.1, 0.4, 0, 21, 970),
    ROW('Salchichas', 2305, 'Salchicha, fresca', 1278.04, 28.1, 10.545, 0.9, 0, 13.12, 1060),
    ROW('Leche semidesnatada', 916, 'Leche, semidesnatada, pasteurizada', 191.8, 1.6, 0.95, 4.6, 0, 3.2, 46),
    ROW('Queso', 2515, 'Queso semicurado, genérico', 1626.3, 30.4, 18.9, 0.5, 0, 29, 670),
    ROW('Nata', 923, 'Nata, líquida, para cocinar, 18% de grasa', 840.078, 19.9894, 12.7, 3.4, 0, 2.51, 45),
    ROW('Mantequilla', 756, 'Mantequilla salada', 3054.4, 82, 55.13, 0.5, 0, 0.7, 870),
    ROW('Yogur natural', 1143, 'Yogur, enriquecido, natural', 265, 3.9, 2.2, 3.9, 0, 3.2, 43),
    ROW('Cacahuete', 2185, 'Cacahuete, crudo, con cáscara', 2265.38, 43.06, 6.75, 7.91, 8.1, 25.23, 2),
    ROW('Nueces', 2201, 'Nuez', 2477.1, 63.28, 7.42, 3.3, 5.2, 14, 3),
    ROW('Almendras', 896, 'Almendra, cruda', 2452.6, 45.22, 4.32, 6.2038, 8.3475, 19.1333, 10.3625),
    ROW('Avellanas', 2219, 'Avellana', 2732.15, 56.25, 4.91, 10.5, 8.22, 12.009, 6),
    ROW('Chocolate blanco', 2650, 'Chocolate blanco', 2278.9, 30.9, 18.2, 58.8, 0.8, 8, 110),
    ROW('Chocolate negro', 782, 'Chocolate negro, con azúcar', 2215, 30, 16.8, 63, NULL, 2, 19),
    ROW('Galletas', 2675, 'Galletas tipo maria', 1996.29, 19, 9.7, 69, 3.1, 7.076, 217)
) AS v (nombre, bedca_id, bedca_nombre, energia_kj, grasas, grasas_saturadas, hidratos, fibra, proteinas, sodio_mg)
JOIN ingredientes i ON i.NOMBRE = v.nombre;

INSERT INTO peso_pieza_ingrediente (INGREDIENTE, PIEZA, GRAMOS)
SELECT i.ID, v.pieza, v.gramos
FROM (VALUES
    ROW('Huevos', 'unidad', 50),
    ROW('Cebolla', 'unidad', 150),
    ROW('Patata', 'unidad', 150),
    ROW('Tomate', 'unidad', 120),
    ROW('Zanahoria', 'unidad', 80),
    ROW('Pimiento', 'unidad', 150),
    ROW('Calabacín', 'unidad', 250),
    ROW('Berenjena', 'unidad', 250),
    ROW('Puerro', 'unidad', 100),
    ROW('Pepino', 'unidad', 250),
    ROW('Ajo', 'diente', 5),
    ROW('Limón', 'unidad', 40),
    ROW('Manzana', 'unidad', 150),
    ROW('Plátano', 'unidad', 120),
    ROW('Naranja', 'unidad', 150),
    ROW('Pera', 'unidad', 150),
    ROW('Fresa', 'unidad', 15),
    ROW('Jamón serrano', 'loncha', 15),
    ROW('Jamón cocido', 'loncha', 20),
    ROW('Bacon', 'loncha', 15),
    ROW('Queso', 'loncha', 20),
    ROW('Salchichas', 'unidad', 50),
    ROW('Yogur natural', 'unidad', 125),
    ROW('Galletas', 'unidad', 6)
) AS v (nombre, pieza, gramos)
JOIN ingredientes i ON i.NOMBRE = v.nombre;
