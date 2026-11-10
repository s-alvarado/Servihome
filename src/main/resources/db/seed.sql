-- SCGO MVP: datos de prueba para CU006, CU007 y CU008.
-- Requiere esquema creado (db/schema.sql). Se carga solo si no hay clientes.
-- Los IDs se resuelven por nombre o con LAST_INSERT_ID(), sin numeros fijos.
-- Las descripciones [Exx] coinciden con la guia de sql/02_insercion_consulta_borrado.sql.
-- Ejecutar una sola vez: repetirlo duplica clientes y solicitudes.

INSERT IGNORE INTO especialidad (nombre) VALUES
  ('Plomeria'), ('Electricidad'), ('Gas'), ('Piscinas'), ('Paisajismo');

INSERT IGNORE INTO tipo_activo (nombre) VALUES
  ('Vehiculo'), ('Maquinaria');

SET @esp_plo = (SELECT id_especialidad FROM especialidad WHERE nombre = 'Plomeria');
SET @esp_ele = (SELECT id_especialidad FROM especialidad WHERE nombre = 'Electricidad');
SET @esp_gas = (SELECT id_especialidad FROM especialidad WHERE nombre = 'Gas');
SET @esp_pis = (SELECT id_especialidad FROM especialidad WHERE nombre = 'Piscinas');
SET @esp_pai = (SELECT id_especialidad FROM especialidad WHERE nombre = 'Paisajismo');
SET @tipo_veh = (SELECT id_tipo FROM tipo_activo WHERE nombre = 'Vehiculo');
SET @tipo_maq = (SELECT id_tipo FROM tipo_activo WHERE nombre = 'Maquinaria');

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Hotel Solar del Cerro', 'B2B', '381-4223344', 'mantenimiento@solardelcerro.com');
SET @cli_hotel = LAST_INSERT_ID();

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Club Atletico Yerba Buena', 'B2B', '381-4256677', 'intendencia@clubyb.org');
SET @cli_club = LAST_INSERT_ID();

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Consorcio Edificio Mirador Norte', 'B2B', '381-4308899', 'consorcio@miradornorte.com');
SET @cli_mirador = LAST_INSERT_ID();

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Colegio San Martin de Porres', 'B2B', '381-4211020', 'administracion@sanmartinporres.edu.ar');
SET @cli_colegio = LAST_INSERT_ID();

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Ana Lopez', 'B2C', '381-5123456', 'ana.lopez@mail.com');
SET @cli_ana = LAST_INSERT_ID();

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Roberto Diaz', 'B2C', '381-5234567', 'rdiaz@mail.com');
SET @cli_roberto = LAST_INSERT_ID();

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Sofia Navarro', 'B2C', '381-5345678', NULL);
SET @cli_sofia = LAST_INSERT_ID();

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Inmobiliaria Del Valle', 'B2B', '381-4012345', 'contacto@delvalle.com');
SET @cli_inmob = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_hotel, 'Ruta 338 km 12, San Javier', 'Hotel con piscina climatizada, parque de 2 ha y caldera central');
SET @pro_hotel1 = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_hotel, 'Av. Aconquija 2100, Yerba Buena', 'Anexo de cabanas con red de gas envasado');
SET @pro_hotel2 = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_club, 'Av. Peron 1800, Yerba Buena', 'Predio deportivo: 2 piscinas, vestuarios y canchas de cesped');
SET @pro_club = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_mirador, 'Av. Sarmiento 450, San Miguel de Tucuman', 'Torre de 14 pisos, tanque de reserva y bombas presurizadoras');
SET @pro_mirador = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_colegio, 'Calle Lamadrid 980, San Miguel de Tucuman', 'Edificio escolar, tablero trifasico y patio arbolado');
SET @pro_colegio1 = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_colegio, 'Camino del Peru 300, Yerba Buena', 'Campo de deportes con riego por aspersion');
SET @pro_colegio2 = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_ana, 'Pje. Los Ceibos 55, Yerba Buena', 'Casa con piscina de fibra y jardin');
SET @pro_ana = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_roberto, 'Av. Mitre 720 3B, San Miguel de Tucuman', 'Departamento, calefon a gas');
SET @pro_roberto1 = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_roberto, 'Lote 14, Barrio Las Yungas, Tafi Viejo', 'Casa de fin de semana con quincho');
SET @pro_roberto2 = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_sofia, 'Calle Salta 1320, San Miguel de Tucuman', NULL);
SET @pro_sofia = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_inmob, 'Av. Belgrano 2600, San Miguel de Tucuman', 'Oficinas comerciales, sala de maquinas');
SET @pro_inmob = LAST_INSERT_ID();

INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_plo, 'Ramiro Sosa', 'Activo');
SET @per_ramiro = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_plo, 'Valeria Ruiz', 'Activo');
SET @per_valeria = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_plo, 'Hernan Villalba', 'Licencia');
SET @per_hernan = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_ele, 'Diego Herrera', 'Activo');
SET @per_diego = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_ele, 'Paula Medina', 'Activo');
SET @per_paula = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_gas, 'Sergio Ledesma', 'Activo');
SET @per_sergio = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_gas, 'Andrea Costa', 'Enfermedad');
SET @per_andrea = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_pis, 'Facundo Ibarra', 'Activo');
SET @per_facundo = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_pai, 'Lucia Paz', 'Activo');
SET @per_lucia = LAST_INSERT_ID();
INSERT INTO personal (id_especialidad, nombre, estado) VALUES (@esp_pai, 'Martin Correa', 'Activo');
SET @per_martin = LAST_INSERT_ID();

INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_veh, 'Camioneta Ranger AC456EF', 'Disponible');
SET @act_ranger = LAST_INSERT_ID();
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_veh, 'Utilitario Kangoo AD789GH', 'Disponible');
SET @act_kangoo = LAST_INSERT_ID();
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_veh, 'Furgon Sprinter AE012IJ', 'Baja');
SET @act_sprinter = LAST_INSERT_ID();
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_maq, 'Hidrolavadora de alta presion', 'Disponible');
SET @act_hidro = LAST_INSERT_ID();
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_maq, 'Desmalezadora a nafta', 'Disponible');
SET @act_desma = LAST_INSERT_ID();
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_maq, 'Detector de fugas de gas', 'Disponible');
SET @act_detector = LAST_INSERT_ID();
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_maq, 'Maquina destapadora electrica', 'Disponible');
SET @act_destap = LAST_INSERT_ID();
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_maq, 'Andamio tubular 6 m', 'Disponible');
SET @act_andamio = LAST_INSERT_ID();
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_maq, 'Generador portatil 5 kVA', 'En Reparacion');
SET @act_generador = LAST_INSERT_ID();

-- OT "E": finalizada con averia. El cliente queda de baja logica, con historial.
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_inmob, @pro_inmob, '2026-10-15', '[E10] Corte de energia en sala de maquinas', 'Programada');
SET @sol_e = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_e, '2026-10-20', '08:00:00', '12:00:00', 'Finalizada');
SET @ot_e = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_e, @per_paula);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_e, @act_generador);
INSERT INTO cierre_orden (id_orden, horas_reales, materiales, estado_devolucion, averia, detalle_averia) VALUES
  (@ot_e, 4.50, 'Contactor 32A, 10 m de cable 4 mm', 'Averiado', TRUE, 'Falla en el regulador de tension del generador');

UPDATE cliente SET activo = FALSE WHERE id_cliente = @cli_inmob;

-- OT "F": finalizada sin averia.
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_club, @pro_club, '2026-10-16', 'Corte de cesped y desmalezado de canchas', 'Programada');
SET @sol_f = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_f, '2026-10-22', '07:00:00', '10:00:00', 'Finalizada');
SET @ot_f = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_f, @per_martin);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_f, @act_desma);
INSERT INTO cierre_orden (id_orden, horas_reales, materiales, estado_devolucion, averia, detalle_averia) VALUES
  (@ot_f, 3.00, 'Tanza 3 mm', 'Buen Estado', FALSE, NULL);

-- OT "A": base de los escenarios de solapamiento.
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_mirador, @pro_mirador, '2026-11-05', '[E12] Reparacion de bomba presurizadora', 'Programada');
SET @sol_a = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_a, '2026-11-16', '08:00:00', '12:00:00', 'Asignada');
SET @ot_a = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_a, @per_ramiro);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_a, @act_ranger);

-- OT "B": lista para cerrar sin averia.
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_colegio, @pro_colegio1, '2026-11-06', '[E07] Revision de tablero trifasico', 'Programada');
SET @sol_b = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_b, '2026-11-16', '14:00:00', '17:00:00', 'Asignada');
SET @ot_b = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_b, @per_diego);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_b, @act_kangoo);

-- OT "C": lista para cerrar con averia. La Camioneta Ranger no esta asignada (R6).
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_hotel, @pro_hotel1, '2026-11-06', '[E08] Limpieza de piscina climatizada y desmalezado del parque', 'Programada');
SET @sol_c = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_c, '2026-11-17', '09:00:00', '13:00:00', 'Asignada');
SET @ot_c = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_c, @per_facundo), (@ot_c, @per_lucia);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_c, @act_hidro), (@ot_c, @act_desma);

-- OT "D": cancelada. Sus recursos no bloquean. La solicitud queda en reprogramacion.
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_hotel, @pro_hotel2, '2026-11-04', '[E13] Control de perdidas en red de gas de cabanas', 'Reprogramacion');
SET @sol_d = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_d, '2026-11-18', '08:00:00', '12:00:00', 'Cancelada');
SET @ot_d = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_d, @per_sergio);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_d, @act_detector);

INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_ana,      @pro_ana,      '2026-11-09', '[E01] Mantenimiento de piscina de fibra antes del verano', 'Pendiente'),
  (@cli_club,     @pro_club,     '2026-11-09', '[E02] Perdida de agua en vestuarios - probar 2026-11-16 10:00-14:00', 'Pendiente'),
  (@cli_roberto,  @pro_roberto1, '2026-11-09', '[E03] Destapar desague de cocina - probar 2026-11-16 12:00-16:00', 'Pendiente'),
  (@cli_mirador,  @pro_mirador,  '2026-11-10', '[E04] Cambio de flotante del tanque - probar 2026-11-19 08:00-12:00', 'Pendiente'),
  (@cli_roberto,  @pro_roberto1, '2026-11-10', '[E11] Revision de calefon a gas - probar 2026-11-18 08:00-12:00', 'Pendiente'),
  (@cli_colegio,  @pro_colegio2, '2026-11-10', '[LIBRE] Reparacion del riego por aspersion', 'Pendiente'),
  (@cli_sofia,    @pro_sofia,    '2026-11-10', '[LIBRE] Instalacion de tomacorrientes en cocina', 'Pendiente'),
  (@cli_roberto,  @pro_roberto2, '2026-11-10', '[LIBRE] Poda y limpieza de parque del quincho', 'Pendiente'),
  (@cli_hotel,    @pro_hotel1,   '2026-11-10', '[LIBRE] Revision de caldera central', 'Pendiente'),
  (@cli_colegio,  @pro_colegio1, '2026-11-10', '[LIBRE] Reemplazo de canillas en sanitarios', 'Pendiente');
