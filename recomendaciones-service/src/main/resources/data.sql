INSERT IGNORE INTO catalogo (tipo, codigo, nombre) VALUES
('RUBRO', 1, 'Inmobiliaria'),
('RUBRO', 2, 'Educación escolar'),
('RUBRO', 3, 'Crédito de consumo'),
('RUBRO', 4, 'Seguros'),
('RUBRO', 5, 'ONG'),
('RUBRO', 6, 'Club deportivo'),
('RUBRO', 7, 'Educación superior'),
('RUBRO', 8, 'Administración de arriendos'),
('RUBRO', 9, 'Educación parvularia'),
('RUBRO', 10, 'Administración de condominio'),
('RUBRO', 11, 'Deporte'),
('RUBRO', 12, 'Educación artística'),
('ESTADO_RECOMENDACION', 1, 'Recibida'),
('ESTADO_RECOMENDACION', 2, 'Contactada'),
('ESTADO_RECOMENDACION', 3, 'En evaluación'),
('ESTADO_RECOMENDACION', 4, 'Se unió'),
('ESTADO_RECOMENDACION', 5, 'Descartada');

INSERT IGNORE INTO recomendacion (id_recomendacion, numero, id_usuario_final, nombre_organizacion, rut_organizacion, id_rubro, id_comuna, fecha, id_estado_recomendacion) VALUES
(1, 'REC-0001', 1, 'Jardín Infantil Pequeños Exploradores', NULL, 9, 1, '2026-08-12 09:00:00', 4),
(2, 'REC-0002', 1, 'Comunidad Edificio Parque Los Olmos', NULL, 10, 1, '2026-09-02 09:00:00', 2),
(3, 'REC-0003', 3, 'Gimnasio Fuerza Sur', NULL, 11, 3, '2026-09-15 09:00:00', 3),
(4, 'REC-0004', 5, 'Academia de Música Clave de Sol', NULL, 12, 5, '2026-09-20 09:00:00', 1),
(5, 'REC-0005', 4, 'Colegio Bosque Nativo', NULL, 2, 4, '2026-09-22 09:00:00', 5),
(6, 'REC-0006', 7, 'Corporación Deportiva Cordillera', NULL, 6, 7, '2026-10-01 09:00:00', 1);

INSERT IGNORE INTO contacto_recomendacion (id_recomendacion, nombre, correo, telefono, autoriza_contacto) VALUES
(1, 'Paula Herrera', 'pherrera@pexploradores.ejemplo', NULL, 1),
(3, 'Raúl Ibáñez', NULL, '+56 9 0000 0301', 1),
(6, 'Ana Valdés', 'avaldes@cdcordillera.ejemplo', NULL, 1);

INSERT IGNORE INTO historial_recomendacion (id_historial, id_recomendacion, id_estado_recomendacion, id_agente, fecha, motivo) VALUES
(1, 1, 4, 12, '2026-09-25 09:00:00', NULL),
(2, 2, 2, 13, '2026-09-10 09:00:00', NULL),
(3, 3, 3, 12, '2026-09-28 09:00:00', NULL),
(4, 5, 5, 13, '2026-09-23 09:00:00', 'La organización ya es cliente de PagoFlex');

