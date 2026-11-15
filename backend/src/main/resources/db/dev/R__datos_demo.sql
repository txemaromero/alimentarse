-- Datos de demostración (solo perfil dev): usuarios, recetas originales, sus ingredientes, likes y valoraciones.
-- Migración repetible e idempotente: Flyway la vuelve a aplicar cuando cambia su contenido.
-- Los consumos, que dependen de la fecha actual, están en afterMigrate__consumos_demo.sql (D-015).
-- Contraseña de las tres cuentas: demo1234

-- Con el email confirmado (D-088), como cuentas que ya se usan.
-- Una cuenta que ya existe no se toca: si este perfil se activase por error contra una base de datos con usuarios
-- reales, no les cambiaría el email ni la contraseña (D-089).
INSERT INTO usuarios (ID, NOMBRE, APELLIDO1, APELLIDO2, EMAIL, PASSWORD, EMAIL_VERIFICADO) VALUES
    ('lucia', 'Lucía', 'Fernández', 'Ruiz', 'lucia@toca.local', '$2a$10$nn1uxKU.Eo2urazTHvOQ/uonck84q5p5kFruUKMz6uGddpD7fjcES', TRUE),
    ('marcos', 'Marcos', 'Gil', 'Navarro', 'marcos@toca.local', '$2a$10$nn1uxKU.Eo2urazTHvOQ/uonck84q5p5kFruUKMz6uGddpD7fjcES', TRUE),
    ('carmen', 'Carmen', 'Ortega', NULL, 'carmen@toca.local', '$2a$10$nn1uxKU.Eo2urazTHvOQ/uonck84q5p5kFruUKMz6uGddpD7fjcES', TRUE)
ON DUPLICATE KEY UPDATE ID = ID;

INSERT INTO usuario_rol (USUARIO, ROL)
SELECT * FROM (
    SELECT v.usuario, r.ID AS rol
    FROM (VALUES ROW('lucia'), ROW('marcos'), ROW('carmen')) AS v (usuario)
    JOIN roles r ON r.NOMBRE = 'USER'
) AS s
ON DUPLICATE KEY UPDATE ROL = s.rol;

-- Recetas con ID fijo (1-10). La fecha de creación solo se fija la primera vez.
INSERT INTO recetas (ID, TITULO, INSTRUCCIONES, FECHA_CREACION, USUARIO, TIPO, PUBLICADA, IMAGEN, TIEMPO, COMENSALES, DIFICULTAD)
SELECT * FROM (
    SELECT v.id, v.titulo, v.instrucciones, NOW() - INTERVAL v.dias DAY AS fecha, v.usuario, t.ID AS tipo,
           v.publicada, v.imagen, v.tiempo, v.comensales, v.dificultad
    FROM (VALUES
        ROW(1, 'Tortilla de patata rellena de jamón y queso',
            '<p>Una tortilla jugosa con un corazón de jamón cocido y queso fundido.</p><ol><li>Pela las patatas y la cebolla, córtalas en láminas finas y sálalas.</li><li>Pocha la patata y la cebolla en el aceite de oliva a fuego medio-bajo unos 20 minutos, hasta que estén tiernas sin llegar a dorarse. Escúrrelas bien.</li><li>Bate los huevos con una pizca de sal, mézclalos con la patata y deja reposar 5 minutos.</li><li>Vierte la mitad de la mezcla en una sartén antiadherente con un hilo de aceite y cúbrela con las lonchas de jamón y de queso, dejando libre el borde.</li><li>Añade el resto de la mezcla, cuaja 3 minutos, dale la vuelta con ayuda de un plato y cuaja otros 2 minutos.</li></ol>',
            30, 'lucia', 'Plato', 1, 'demo-tortilla.jpg', 45, 4, 'Media'),
        ROW(2, 'Puré de calabaza y puerro',
            '<p>Crema suave y anaranjada, ideal para empezar una comida de otoño.</p><ol><li>Limpia los puerros y córtalos en rodajas. Pela la calabaza y la patata y trocéalas.</li><li>Rehoga el puerro en el aceite durante 5 minutos sin que tome color.</li><li>Añade la calabaza y la patata, cubre con agua justa y cuece 25 minutos.</li><li>Tritura con la nata hasta que quede fino y ajusta de sal y pimienta.</li></ol>',
            25, 'marcos', 'Entrante', 0, 'demo-pure-calabaza.jpg', 40, 4, 'Fácil'),
        ROW(3, 'Boquerones fritos',
            '<p>Un clásico de barra: crujientes por fuera y tiernos por dentro.</p><ol><li>Limpia los boquerones quitando la cabeza y las tripas, lávalos y sécalos bien con papel de cocina.</li><li>Sálalos y pásalos por harina, sacudiendo el exceso.</li><li>Fríelos en tandas pequeñas en aceite de girasol muy caliente hasta que estén dorados.</li><li>Escúrrelos sobre papel absorbente y sírvelos al momento con gajos de limón.</li></ol>',
            28, 'lucia', 'Entrante', 1, 'demo-boquerones.jpg', 30, 3, 'Fácil'),
        ROW(4, 'Crema catalana',
            '<p>Postre cremoso perfumado con canela y limón, con su capa de azúcar quemado.</p><ol><li>Calienta la leche con la rama de canela y la piel del limón. Cuando hierva, apaga el fuego y deja infusionar 10 minutos.</li><li>Bate las yemas con 100 gramos de azúcar y la maicena hasta que blanqueen.</li><li>Cuela la leche y añádela poco a poco a las yemas sin dejar de remover.</li><li>Cocina a fuego suave, removiendo, hasta que espese sin llegar a hervir.</li><li>Reparte en cazuelitas y enfría al menos 2 horas. Antes de servir, cubre con el azúcar restante y quémalo con un soplete.</li></ol>',
            20, 'lucia', 'Postre', 1, 'demo-crema-catalana.jpg', 40, 6, 'Media'),
        ROW(5, 'Rollos de canela con chocolate blanco',
            '<p>Bollos tiernos enrollados con azúcar moreno y canela, terminados con chocolate blanco.</p><ol><li>Templa la leche y disuelve en ella la levadura. Mézclala con la harina, el huevo, la sal y 50 gramos de mantequilla blanda.</li><li>Amasa 10 minutos hasta obtener una masa elástica y déjala levar tapada durante 1 hora.</li><li>Estira la masa en un rectángulo, úntala con el resto de la mantequilla y espolvorea el azúcar moreno mezclado con la canela.</li><li>Enrolla por el lado largo, corta 12 porciones, colócalas en una bandeja y deja levar 30 minutos más.</li><li>Hornea a 180 °C unos 20 minutos. Funde el chocolate blanco y viértelo por encima cuando estén templados.</li></ol>',
            15, 'marcos', 'Desayuno', 1, 'demo-rollos-canela.jpg', 120, 6, 'Difícil'),
        ROW(6, 'Pollo con tallarines',
            '<p>Plato único rápido con salsa de tomate casera.</p><ol><li>Corta el pollo en tiras, salpimiéntalo y dóralo en una sartén con el aceite. Resérvalo.</li><li>En la misma sartén sofríe la cebolla picada y el ajo laminado. Añade el tomate rallado y cocina 10 minutos.</li><li>Cuece los tallarines en agua con sal el tiempo que indique el fabricante y escúrrelos.</li><li>Incorpora el pollo y la pasta a la salsa, espolvorea orégano y saltea un minuto antes de servir.</li></ol>',
            12, 'marcos', 'Plato', 1, 'demo-pollo-tallarines.jpg', 35, 4, 'Fácil'),
        ROW(7, 'Espárragos con tomate',
            '<p>Entrante ligero de temporada en menos de 20 minutos.</p><ol><li>Quita la parte leñosa de los espárragos.</li><li>Saltéalos en una sartén con el aceite y el ajo laminado unos 8 minutos, hasta que estén tiernos.</li><li>Añade el tomate en dados, sala y cocina 3 minutos más.</li><li>Sirve con perejil picado por encima.</li></ol>',
            10, 'carmen', 'Entrante', 1, 'demo-esparragos.jpg', 20, 2, 'Fácil'),
        ROW(8, 'Lentejas estofadas',
            '<p>Legumbre de cuchara de toda la vida, con chorizo y verduras.</p><ol><li>Pon las lentejas en una olla con agua fría, la hoja de laurel, la zanahoria en rodajas y el chorizo en rodajas.</li><li>Mientras tanto, sofríe en el aceite la cebolla, el ajo y el pimiento picados. Retira del fuego, añade el pimentón y remueve.</li><li>Incorpora el sofrito a la olla junto con la patata cascada en trozos.</li><li>Cuece a fuego suave unos 40 minutos, hasta que las lentejas estén tiernas. Ajusta de sal y deja reposar 5 minutos.</li></ol>',
            6, 'carmen', 'Plato', 1, 'demo-lentejas.jpg', 60, 4, 'Media'),
        ROW(9, 'Ensalada de garbanzos',
            '<p>Ensalada completa y fresquita para los días de calor.</p><ol><li>Enjuaga y escurre los garbanzos cocidos.</li><li>Pica el tomate, el pepino y la cebolla en dados pequeños.</li><li>Mezcla todo en un bol con el atún desmigado.</li><li>Aliña con aceite, vinagre y sal justo antes de servir.</li></ol>',
            4, 'carmen', 'Plato', 1, 'demo-ensalada-garbanzos.jpg', 15, 2, 'Fácil'),
        ROW(10, 'Batido de plátano y fresa',
            '<p>Batido cremoso sin azúcar añadido.</p><ol><li>Lava las fresas y quítales el rabito. Pela los plátanos y trocéalos.</li><li>Tritura la fruta con la leche y el yogur hasta que quede cremoso.</li><li>Sirve bien frío.</li></ol>',
            2, 'lucia', 'Bebida', 1, 'demo-batido.jpg', 5, 2, 'Fácil')
    ) AS v (id, titulo, instrucciones, dias, usuario, tipo, publicada, imagen, tiempo, comensales, dificultad)
    JOIN tipo_receta t ON t.NOMBRE = v.tipo
) AS s
ON DUPLICATE KEY UPDATE
    TITULO = s.titulo,
    INSTRUCCIONES = s.instrucciones,
    USUARIO = s.usuario,
    TIPO = s.tipo,
    PUBLICADA = s.publicada,
    IMAGEN = s.imagen,
    TIEMPO = s.tiempo,
    COMENSALES = s.comensales,
    DIFICULTAD = s.dificultad;

-- Galería (D-083): la foto principal de cada receta y dos más de la tortilla
DELETE FROM foto_receta WHERE RECETA BETWEEN 1 AND 10;

INSERT INTO foto_receta (RECETA, NOMBRE, ORDEN, DESCRIPCION)
SELECT ID, IMAGEN, 0, NULL FROM recetas WHERE ID BETWEEN 1 AND 10 AND IMAGEN IS NOT NULL;

INSERT INTO foto_receta (RECETA, NOMBRE, ORDEN, DESCRIPCION) VALUES
    (1, 'demo-tortilla-corte.jpg', 1, 'La tortilla cortada, con el relleno de jamón cocido y queso'),
    (1, 'demo-tortilla-ingredientes.jpg', 2, 'Los ingredientes: patatas, huevos, cebolla, jamón cocido y queso');

DELETE FROM ingrediente_receta WHERE RECETA BETWEEN 1 AND 10;

INSERT INTO ingrediente_receta (INGREDIENTE, RECETA, CANTIDAD)
SELECT i.ID, v.receta, v.cantidad
FROM (VALUES
    ROW(1, 'Patata', '600 gramos'),
    ROW(1, 'Huevos', '6 unidades'),
    ROW(1, 'Cebolla', '1 unidad'),
    ROW(1, 'Aceite de oliva', '200 mililitros'),
    ROW(1, 'Sal', '1 pizca'),
    ROW(1, 'Jamón cocido', '4 lonchas'),
    ROW(1, 'Queso', '4 lonchas'),

    ROW(2, 'Calabaza', '800 gramos'),
    ROW(2, 'Puerro', '2 unidades'),
    ROW(2, 'Patata', '1 unidad'),
    ROW(2, 'Nata', '100 mililitros'),
    ROW(2, 'Aceite de oliva', '2 cucharadas'),
    ROW(2, 'Sal', 'al gusto'),
    ROW(2, 'Pimienta', '1 pizca'),
    ROW(2, 'Agua', 'la necesaria'),

    ROW(3, 'Boquerón', '500 gramos'),
    ROW(3, 'Harina', '150 gramos'),
    ROW(3, 'Aceite de girasol', '500 mililitros'),
    ROW(3, 'Sal', 'al gusto'),
    ROW(3, 'Limón', '1 unidad'),

    ROW(4, 'Leche semidesnatada', '1 litro'),
    ROW(4, 'Huevos', '6 yemas'),
    ROW(4, 'Azúcar', '150 gramos'),
    ROW(4, 'Maicena', '40 gramos'),
    ROW(4, 'Canela', '1 rama'),
    ROW(4, 'Limón', 'la piel de 1 unidad'),

    ROW(5, 'Harina', '500 gramos'),
    ROW(5, 'Leche semidesnatada', '250 mililitros'),
    ROW(5, 'Mantequilla', '100 gramos'),
    ROW(5, 'Azúcar moreno', '120 gramos'),
    ROW(5, 'Canela', '2 cucharadas'),
    ROW(5, 'Levadura', '20 gramos'),
    ROW(5, 'Huevos', '1 unidad'),
    ROW(5, 'Chocolate blanco', '100 gramos'),
    ROW(5, 'Sal', '1 pizca'),

    ROW(6, 'Pollo', '400 gramos'),
    ROW(6, 'Tallarines', '320 gramos'),
    ROW(6, 'Cebolla', '1 unidad'),
    ROW(6, 'Ajo', '2 dientes'),
    ROW(6, 'Tomate', '3 unidades'),
    ROW(6, 'Aceite de oliva', '3 cucharadas'),
    ROW(6, 'Sal', 'al gusto'),
    ROW(6, 'Pimienta', '1 pizca'),
    ROW(6, 'Orégano', '1 cucharadita'),
    ROW(6, 'Agua', 'para cocer la pasta'),

    ROW(7, 'Espárragos', '1 manojo'),
    ROW(7, 'Tomate', '2 unidades'),
    ROW(7, 'Ajo', '1 diente'),
    ROW(7, 'Aceite de oliva', '2 cucharadas'),
    ROW(7, 'Sal', 'al gusto'),
    ROW(7, 'Perejil', '1 ramita'),

    ROW(8, 'Lentejas', '300 gramos'),
    ROW(8, 'Chorizo', '1 unidad'),
    ROW(8, 'Zanahoria', '2 unidades'),
    ROW(8, 'Patata', '2 unidades'),
    ROW(8, 'Cebolla', '1 unidad'),
    ROW(8, 'Ajo', '2 dientes'),
    ROW(8, 'Pimiento', 'medio'),
    ROW(8, 'Pimentón', '1 cucharadita'),
    ROW(8, 'Laurel', '1 hoja'),
    ROW(8, 'Aceite de oliva', '3 cucharadas'),
    ROW(8, 'Sal', 'al gusto'),
    ROW(8, 'Agua', '1,5 litros'),

    ROW(9, 'Garbanzos', '400 gramos cocidos'),
    ROW(9, 'Tomate', '2 unidades'),
    ROW(9, 'Pepino', '1 unidad'),
    ROW(9, 'Cebolla', 'media'),
    ROW(9, 'Atún', '2 latas'),
    ROW(9, 'Aceite de oliva', '4 cucharadas'),
    ROW(9, 'Vinagre', '1 cucharada'),
    ROW(9, 'Sal', 'al gusto'),

    ROW(10, 'Plátano', '2 unidades'),
    ROW(10, 'Fresa', '200 gramos'),
    ROW(10, 'Leche semidesnatada', '400 mililitros'),
    ROW(10, 'Yogur natural', '1 unidad')
) AS v (receta, nombre, cantidad)
JOIN ingredientes i ON i.NOMBRE = v.nombre;

-- Likes solo en recetas publicadas
DELETE FROM like_receta WHERE RECETA BETWEEN 1 AND 10 AND USUARIO IN ('lucia', 'marcos', 'carmen');

INSERT INTO like_receta (USUARIO, RECETA) VALUES
    ('lucia', 1), ('marcos', 1), ('carmen', 1),
    ('lucia', 8), ('marcos', 8),
    ('marcos', 4), ('carmen', 4),
    ('lucia', 5),
    ('carmen', 6),
    ('carmen', 10),
    ('marcos', 3);

-- Valoraciones (D-077): solo en recetas publicadas y nunca de su propio autor
DELETE FROM valoracion_receta WHERE RECETA BETWEEN 1 AND 10 AND USUARIO IN ('lucia', 'marcos', 'carmen');

INSERT INTO valoracion_receta (USUARIO, RECETA, NOTA, COMENTARIO, FECHA) VALUES
    ('marcos', 1, 5, 'Jugosa y con mucho sabor. El relleno de queso la hace especial.', NOW() - INTERVAL 20 DAY),
    ('carmen', 1, 4, 'Muy rica, aunque la próxima vez le pondré menos aceite.', NOW() - INTERVAL 9 DAY),
    ('marcos', 3, 4, NULL, NOW() - INTERVAL 18 DAY),
    ('marcos', 4, 5, NULL, NOW() - INTERVAL 15 DAY),
    ('carmen', 4, 4, 'El azúcar quemado queda perfecto con un soplete de cocina.', NOW() - INTERVAL 6 DAY),
    ('lucia', 5, 4, 'Laboriosa, pero merece la pena. Mejor recién hechos.', NOW() - INTERVAL 11 DAY),
    ('carmen', 5, 3, NULL, NOW() - INTERVAL 4 DAY),
    ('lucia', 6, 4, NULL, NOW() - INTERVAL 8 DAY),
    ('carmen', 6, 3, 'Le faltaba un poco de sal, pero la salsa de tomate está muy buena.', NOW() - INTERVAL 3 DAY),
    ('lucia', 7, 3, NULL, NOW() - INTERVAL 7 DAY),
    ('lucia', 8, 5, 'Como las de casa de toda la vida.', NOW() - INTERVAL 5 DAY),
    ('marcos', 8, 4, NULL, NOW() - INTERVAL 2 DAY),
    ('marcos', 9, 4, 'Fresca y rápida para los días de calor.', NOW() - INTERVAL 10 DAY),
    ('carmen', 10, 5, NULL, NOW() - INTERVAL 1 DAY);
