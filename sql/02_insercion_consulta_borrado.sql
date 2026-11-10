-- =====================================================================
-- SCGO - ServiHome
-- Script 02: Insercion, consulta y borrado de registros de prueba
-- Requiere haber ejecutado 01_creacion_tablas.sql
--
-- La seccion 1 carga el juego ampliado para probar el prototipo (CU006,
-- CU007 y CU008). Los catalogos usan INSERT IGNORE y los IDs se obtienen
-- por nombre o con LAST_INSERT_ID(). Ejecutar la seccion 1 una sola vez:
-- si se repite, duplica clientes y solicitudes.
--
-- Las secciones 1 y 2 dejan los datos en la base. La seccion 3 los borra.
-- El mismo juego esta en src/main/resources/db/seed.sql: si la seccion 3
-- vacia las tablas, el proximo arranque de la aplicacion lo vuelve a cargar
-- cuando no hay clientes.
--
-- Las descripciones de solicitudes llevan una etiqueta [Exx].
-- =====================================================================
--
-- GUIA DE ESCENARIOS (los IDs reales salen en las consultas de la seccion 2)
-- ---------------------------------------------------------------------
-- Ocupacion de recursos ya cargada:
--   OT "A"  2026-11-16 08:00-12:00 ASIGNADA   -> Ramiro Sosa + Camioneta Ranger
--   OT "B"  2026-11-16 14:00-17:00 ASIGNADA   -> Diego Herrera + Utilitario Kangoo
--   OT "C"  2026-11-17 09:00-13:00 ASIGNADA   -> Facundo Ibarra + Lucia Paz
--                                                + Hidrolavadora + Desmalezadora
--   OT "D"  2026-11-18 08:00-12:00 CANCELADA  -> Sergio Ledesma + Detector de fugas
--   OT "E"  2026-10-20 FINALIZADA con averia  -> Generador portatil quedo En Reparacion
--   OT "F"  2026-10-22 FINALIZADA sin averia
--
-- CU006 - Generar orden
--   E01  Solicitud [E01] Pendiente -> genera la orden sin problemas.
--   E12  Solicitud de la OT "A" (Programada) -> debe rechazarse: no esta Pendiente.
--   E13  Solicitud de la OT "D" (Reprogramacion) -> debe rechazarse: no esta Pendiente.
--
-- CU007 - Asignar recursos (R1 solapamiento, R2 disponibilidad)
--   E02  [E02] orden 2026-11-16 10:00-14:00 + Ramiro Sosa       -> RECHAZO (se cruza con A)
--   E02b [E02] misma orden + Camioneta Ranger                   -> RECHAZO (se cruza con A)
--   E03  [E03] orden 2026-11-16 12:00-16:00 + Ramiro Sosa       -> OK (contiguo a A, R1 estricta)
--        ...y + Diego Herrera                                   -> RECHAZO (se cruza con B 14-17)
--   E04  [E04] orden 2026-11-19 08:00-12:00 + Ramiro Sosa       -> OK (otro dia)
--   E05  cualquier orden nueva + Hernan Villalba (Licencia)
--        o Andrea Costa (Enfermedad)                            -> RECHAZO (personal no operativo)
--   E06  cualquier orden nueva + Furgon Sprinter (Baja)
--        o Generador portatil (En Reparacion)                   -> RECHAZO (activo no operativo)
--   E11  [E11] orden 2026-11-18 08:00-12:00 + Sergio Ledesma    -> OK (la OT D esta Cancelada)
--
-- CU008 - Cerrar orden (R4 transaccion, R5 detalle de averia, R6 activos)
--   E07  OT "B" cierre sin averia                               -> FINALIZADA
--   E08  OT "C" cierre con averia sobre la Hidrolavadora        -> FINALIZADA + activo En Reparacion
--   E09  OT "C" cierre con averia sobre la Camioneta Ranger     -> RECHAZO R6 (no asignada a C)
--   E10  OT "E" (ya Finalizada) intentar cerrarla de nuevo      -> RECHAZO
--
-- Solicitudes [LIBRE]: pendientes extra para probar a gusto.
-- Cliente "Inmobiliaria Del Valle": dado de baja logica (RF001), con historial.
-- =====================================================================

USE servihome;

-- =====================================================================
-- 1) INSERCION DE DATOS DE PRUEBA
-- =====================================================================

-- Catalogos (no duplica si ya existen: nombre es UNIQUE)
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

-- Clientes (B2B y B2C)
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
  ('Sofia Navarro', 'B2C', '381-5345678', NULL);   -- sin email (campo opcional)
SET @cli_sofia = LAST_INSERT_ID();

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Inmobiliaria Del Valle', 'B2B', '381-4012345', 'contacto@delvalle.com');
SET @cli_inmob = LAST_INSERT_ID();

-- Propiedades (varios clientes con mas de una - RF002)
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
  (@cli_sofia, 'Calle Salta 1320, San Miguel de Tucuman', NULL);   -- sin caracteristicas
SET @pro_sofia = LAST_INSERT_ID();

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (@cli_inmob, 'Av. Belgrano 2600, San Miguel de Tucuman', 'Oficinas comerciales, sala de maquinas');
SET @pro_inmob = LAST_INSERT_ID();

-- Personal operativo (todas las especialidades y los 3 estados)
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

-- Activos fisicos (los 3 estados)
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
-- Queda En Reparacion por la averia registrada en la OT "E" (historial)
INSERT INTO activo (id_tipo, descripcion, estado) VALUES (@tipo_maq, 'Generador portatil 5 kVA', 'En Reparacion');
SET @act_generador = LAST_INSERT_ID();

-- Historial: ordenes ya finalizadas (OT "E" y OT "F")
-- OT "E": finalizada con averia; luego el cliente se da de baja logica.
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

UPDATE cliente SET activo = FALSE WHERE id_cliente = @cli_inmob;   -- baja logica (RF001)

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

-- Agenda vigente: ordenes ASIGNADA y CANCELADA (ocupan o liberan recursos)
-- OT "A": base de los escenarios de solapamiento E02/E03/E04.
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_mirador, @pro_mirador, '2026-11-05', '[E12] Reparacion de bomba presurizadora', 'Programada');
SET @sol_a = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_a, '2026-11-16', '08:00:00', '12:00:00', 'Asignada');
SET @ot_a = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_a, @per_ramiro);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_a, @act_ranger);

-- OT "B": lista para cerrar SIN averia (E07).
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_colegio, @pro_colegio1, '2026-11-06', '[E07] Revision de tablero trifasico', 'Programada');
SET @sol_b = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_b, '2026-11-16', '14:00:00', '17:00:00', 'Asignada');
SET @ot_b = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_b, @per_diego);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_b, @act_kangoo);

-- OT "C": lista para cerrar CON averia (E08) y probar R6 (E09).
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_hotel, @pro_hotel1, '2026-11-06', '[E08] Limpieza de piscina climatizada y desmalezado del parque', 'Programada');
SET @sol_c = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_c, '2026-11-17', '09:00:00', '13:00:00', 'Asignada');
SET @ot_c = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_c, @per_facundo), (@ot_c, @per_lucia);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_c, @act_hidro),   (@ot_c, @act_desma);

-- OT "D": cancelada; sus recursos NO deben bloquear (E11). Solicitud a reprogramar (E13).
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (@cli_hotel, @pro_hotel2, '2026-11-04', '[E13] Control de perdidas en red de gas de cabanas', 'Reprogramacion');
SET @sol_d = LAST_INSERT_ID();
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (@sol_d, '2026-11-18', '08:00:00', '12:00:00', 'Cancelada');
SET @ot_d = LAST_INSERT_ID();
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES (@ot_d, @per_sergio);
INSERT INTO asignacion_activo   (id_orden, id_activo)   VALUES (@ot_d, @act_detector);

-- Solicitudes PENDIENTES (entrada del CU006)
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


-- =====================================================================
-- 2) CONSULTAS DE PRUEBA
-- =====================================================================

-- Consulta A: Agenda de ordenes con cliente, propiedad y recursos asignados.
-- Demuestra el cruce de las entidades principales del sistema.
SELECT
    ot.id_orden                         AS orden,
    c.razon_social                      AS cliente,
    p.direccion                         AS propiedad,
    ot.fecha_servicio                   AS fecha,
    CONCAT(ot.hora_inicio,' - ',ot.hora_fin) AS horario,
    GROUP_CONCAT(DISTINCT per.nombre SEPARATOR ', ')      AS cuadrilla,
    GROUP_CONCAT(DISTINCT act.descripcion SEPARATOR ', ') AS recursos,
    ot.estado
FROM orden_trabajo ot
JOIN solicitud s            ON ot.id_solicitud = s.id_solicitud
JOIN cliente c              ON s.id_cliente    = c.id_cliente
JOIN propiedad p            ON s.id_propiedad  = p.id_propiedad
LEFT JOIN asignacion_personal ap ON ot.id_orden = ap.id_orden
LEFT JOIN personal per           ON ap.id_personal = per.id_personal
LEFT JOIN asignacion_activo   aa ON ot.id_orden = aa.id_orden
LEFT JOIN activo act             ON aa.id_activo  = act.id_activo
GROUP BY ot.id_orden, c.razon_social, p.direccion,
         ot.fecha_servicio, ot.hora_inicio, ot.hora_fin, ot.estado;

-- Consulta B: VALIDACION DE SOLAPAMIENTO (nucleo del RF007 / CU007, escenario E02).
-- Antes de asignar a Ramiro Sosa a una nueva orden el 2026-11-16 de 10:00 a 14:00,
-- se verifica si ya tiene una orden que se cruce en fecha y rango horario.
-- La OT "A" ocupa 08:00-12:00. Si devuelve filas, la asignacion debe bloquearse.
SELECT ot.id_orden, ot.fecha_servicio, ot.hora_inicio, ot.hora_fin
FROM asignacion_personal ap
JOIN orden_trabajo ot ON ap.id_orden = ot.id_orden
WHERE ap.id_personal = @per_ramiro
  AND ot.fecha_servicio = '2026-11-16'
  AND ot.estado = 'Asignada'
  AND ot.hora_inicio < '14:00:00'   -- se solapan si: inicio_existente < fin_nuevo
  AND ot.hora_fin    > '10:00:00';  --            y    fin_existente   > inicio_nuevo

-- Consulta C: Activos que NO estan disponibles para asignar (RF007 / RF009).
-- En este juego: Furgon Sprinter (Baja) y Generador portatil (En Reparacion).
SELECT a.id_activo, a.descripcion, t.nombre AS tipo, a.estado
FROM activo a
JOIN tipo_activo t ON a.id_tipo = t.id_tipo
WHERE a.estado <> 'Disponible';

-- IDs reales para seguir la guia desde la consola del prototipo.
SELECT s.id_solicitud, s.estado, c.razon_social AS cliente, s.descripcion
FROM solicitud s
JOIN cliente c ON s.id_cliente = c.id_cliente
ORDER BY s.estado, s.id_solicitud;

SELECT ot.id_orden, ot.estado, ot.fecha_servicio,
       CONCAT(ot.hora_inicio, ' - ', ot.hora_fin) AS horario,
       GROUP_CONCAT(DISTINCT CONCAT(per.id_personal, ':', per.nombre) SEPARATOR ', ')    AS personal,
       GROUP_CONCAT(DISTINCT CONCAT(act.id_activo, ':', act.descripcion) SEPARATOR ', ') AS activos
FROM orden_trabajo ot
LEFT JOIN asignacion_personal ap ON ot.id_orden = ap.id_orden
LEFT JOIN personal per           ON ap.id_personal = per.id_personal
LEFT JOIN asignacion_activo aa   ON ot.id_orden = aa.id_orden
LEFT JOIN activo act             ON aa.id_activo = act.id_activo
GROUP BY ot.id_orden, ot.estado, ot.fecha_servicio, ot.hora_inicio, ot.hora_fin
ORDER BY ot.fecha_servicio, ot.hora_inicio;

SELECT p.id_personal, p.nombre, e.nombre AS especialidad, p.estado
FROM personal p JOIN especialidad e ON p.id_especialidad = e.id_especialidad
ORDER BY p.id_personal;

SELECT a.id_activo, a.descripcion, t.nombre AS tipo, a.estado
FROM activo a JOIN tipo_activo t ON a.id_tipo = t.id_tipo
ORDER BY a.id_activo;


-- =====================================================================
-- 3) BAJA LOGICA, BORRADO DE DATOS DE PRUEBA Y VERIFICACION
-- =====================================================================

-- Baja logica (RF001): Inmobiliaria Del Valle quedo inactiva en la seccion 1,
-- despues de cerrar la OT "E". La fila sigue, con su propiedad y su historial.
-- El UPDATE de esa seccion es:
--   UPDATE cliente SET activo = FALSE WHERE id_cliente = @cli_inmob;
SELECT id_cliente, razon_social, activo FROM cliente ORDER BY id_cliente;

-- Borrado fisico: solo para limpiar los datos de prueba.
-- Se borra en orden inverso a la creacion para respetar las claves foraneas.
-- Despues de esto la base queda vacia y el proximo arranque puede recargar seed.sql.

DELETE FROM cierre_orden;
DELETE FROM asignacion_activo;
DELETE FROM asignacion_personal;
DELETE FROM orden_trabajo;
DELETE FROM solicitud;
DELETE FROM activo;
DELETE FROM personal;
DELETE FROM propiedad;
DELETE FROM cliente;
DELETE FROM tipo_activo;
DELETE FROM especialidad;

-- Verificacion: todas deben devolver 0 filas
SELECT COUNT(*) AS clientes        FROM cliente;
SELECT COUNT(*) AS ordenes         FROM orden_trabajo;
SELECT COUNT(*) AS asig_personal   FROM asignacion_personal;
SELECT COUNT(*) AS asig_activo     FROM asignacion_activo;
