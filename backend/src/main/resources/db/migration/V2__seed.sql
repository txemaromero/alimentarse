-- Datos iniciales comunes a todos los entornos: roles, tipos de receta, grupos de alimentos e ingredientes.
-- Las raciones semanales son los valores iniciales de la especificación y se pueden editar.
-- Las clasificaciones de ingredientes discutibles están explicadas en docs/DECISIONES.md (D-014).

INSERT INTO roles (NOMBRE) VALUES ('USER'), ('ADMIN');

INSERT INTO tipo_receta (NOMBRE) VALUES ('Plato'), ('Entrante'), ('Postre'), ('Bebida'), ('Desayuno'), ('Salsa');

-- RACIONES_SEMANA NULL = sin límite (∞)
INSERT INTO grupo_ingrediente (NOMBRE, RACIONES_SEMANA) VALUES
    ('Esenciales', NULL),
    ('Especias', NULL),
    ('Verduras', NULL),
    ('Hortalizas', NULL),
    ('Fruta', NULL),
    ('Legumbres', 4),
    ('Pasta', 4),
    ('Arroz', 4),
    ('Pescado', 4),
    ('Huevos', 4),
    ('Carne', 3),
    ('Carne grasa', 2),
    ('Leches y derivados', 14),
    ('Frutos secos', 7),
    ('Dulces', 1);

INSERT INTO ingredientes (NOMBRE, GRUPO)
SELECT v.nombre, g.ID
FROM (VALUES
    ROW('Aceite de oliva', 'Esenciales'),
    ROW('Aceite de girasol', 'Esenciales'),
    ROW('Sal', 'Esenciales'),
    ROW('Agua', 'Esenciales'),
    ROW('Azúcar', 'Esenciales'),
    ROW('Azúcar moreno', 'Esenciales'),
    ROW('Harina', 'Esenciales'),
    ROW('Vinagre', 'Esenciales'),
    ROW('Levadura', 'Esenciales'),
    ROW('Maicena', 'Esenciales'),

    ROW('Canela', 'Especias'),
    ROW('Pimienta', 'Especias'),
    ROW('Pimentón', 'Especias'),
    ROW('Orégano', 'Especias'),
    ROW('Comino', 'Especias'),
    ROW('Laurel', 'Especias'),
    ROW('Perejil', 'Especias'),
    ROW('Nuez moscada', 'Especias'),

    ROW('Espárragos', 'Verduras'),
    ROW('Espinacas', 'Verduras'),
    ROW('Lechuga', 'Verduras'),
    ROW('Brócoli', 'Verduras'),
    ROW('Judías verdes', 'Verduras'),
    ROW('Champiñones', 'Verduras'),

    ROW('Patata', 'Hortalizas'),
    ROW('Cebolla', 'Hortalizas'),
    ROW('Ajo', 'Hortalizas'),
    ROW('Calabaza', 'Hortalizas'),
    ROW('Puerro', 'Hortalizas'),
    ROW('Berenjena', 'Hortalizas'),
    ROW('Tomate', 'Hortalizas'),
    ROW('Zanahoria', 'Hortalizas'),
    ROW('Pimiento', 'Hortalizas'),
    ROW('Calabacín', 'Hortalizas'),
    ROW('Pepino', 'Hortalizas'),

    ROW('Limón', 'Fruta'),
    ROW('Manzana', 'Fruta'),
    ROW('Plátano', 'Fruta'),
    ROW('Naranja', 'Fruta'),
    ROW('Fresa', 'Fruta'),
    ROW('Pera', 'Fruta'),

    ROW('Lentejas', 'Legumbres'),
    ROW('Garbanzos', 'Legumbres'),
    ROW('Alubias blancas', 'Legumbres'),
    ROW('Guisantes', 'Legumbres'),

    ROW('Tallarines', 'Pasta'),
    ROW('Macarrones', 'Pasta'),
    ROW('Espaguetis', 'Pasta'),

    ROW('Arroz', 'Arroz'),

    ROW('Atún', 'Pescado'),
    ROW('Boquerón', 'Pescado'),
    ROW('Sardina', 'Pescado'),
    ROW('Merluza', 'Pescado'),
    ROW('Salmón', 'Pescado'),
    ROW('Bacalao', 'Pescado'),

    ROW('Huevos', 'Huevos'),

    ROW('Pollo', 'Carne'),
    ROW('Ternera', 'Carne'),
    ROW('Carrillada', 'Carne'),
    ROW('Lomo de cerdo', 'Carne'),

    ROW('Bacon', 'Carne grasa'),
    ROW('Chorizo', 'Carne grasa'),
    ROW('Jamón serrano', 'Carne grasa'),
    ROW('Jamón cocido', 'Carne grasa'),
    ROW('Salchichas', 'Carne grasa'),

    ROW('Leche semidesnatada', 'Leches y derivados'),
    ROW('Queso', 'Leches y derivados'),
    ROW('Nata', 'Leches y derivados'),
    ROW('Mantequilla', 'Leches y derivados'),
    ROW('Yogur natural', 'Leches y derivados'),

    ROW('Cacahuete', 'Frutos secos'),
    ROW('Nueces', 'Frutos secos'),
    ROW('Almendras', 'Frutos secos'),
    ROW('Avellanas', 'Frutos secos'),

    ROW('Chocolate blanco', 'Dulces'),
    ROW('Chocolate negro', 'Dulces'),
    ROW('Galletas', 'Dulces')
) AS v (nombre, grupo)
JOIN grupo_ingrediente g ON g.NOMBRE = v.grupo;
