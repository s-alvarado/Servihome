-- =====================================================================
-- SCGO - ServiHome
-- Script 02: Insercion, consulta y borrado de registros de prueba
-- Requiere haber ejecutado 01_creacion_tablas.sql
-- =====================================================================

USE servihome;

-- =====================================================================
-- 1) INSERCION DE DATOS DE PRUEBA
-- =====================================================================

-- Catalogos
INSERT INTO especialidad (nombre) VALUES
  ('Plomeria'), ('Electricidad'), ('Gas'), ('Piscinas'), ('Paisajismo');

INSERT INTO tipo_activo (nombre) VALUES
  ('Vehiculo'), ('Maquinaria');

-- Clientes (B2B y B2C)
INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Consorcio Torres del Parque', 'B2B', '381-4001122', 'admin@torresparque.com'),
  ('Juan Perez',                  'B2C', '381-5559876', 'jperez@mail.com');

-- Propiedades (un cliente puede tener varias - RF002)
INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (1, 'Av. Mate de Luna 1500, Tucuman', 'Predio con 2 piscinas y parque'),
  (1, 'Calle San Lorenzo 250, Tucuman', 'Sala de maquinas, grupo electrogeno'),
  (2, 'Bv. Aconquija 880, Yerba Buena', 'Vivienda con jardin');

-- Personal operativo
INSERT INTO personal (id_especialidad, nombre, estado) VALUES
  (4, 'Carlos Gomez',   'Activo'),    -- Piscinas
  (5, 'Marta Diaz',     'Activo'),    -- Paisajismo
  (2, 'Luis Fernandez', 'Licencia');  -- Electricidad (no disponible)

-- Activos fisicos
INSERT INTO activo (id_tipo, descripcion, estado) VALUES
  (1, 'Camioneta Hilux AB123CD',          'Disponible'),
  (2, 'Cortadora de cesped industrial',   'Disponible'),
  (2, 'Bomba sumergible para piscina',    'En Reparacion'); -- inoperativa

-- Solicitud de servicio (estado inicial Pendiente - RF005)
INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (1, 1, '2026-06-05', 'Mantenimiento mensual de piscinas y parque', 'Pendiente');

-- Orden de Trabajo generada a partir de la solicitud (RF006)
INSERT INTO orden_trabajo (id_solicitud, fecha_servicio, hora_inicio, hora_fin, estado) VALUES
  (1, '2026-06-10', '08:00:00', '12:00:00', 'Asignada');

-- La solicitud pasa a 'Programada'
UPDATE solicitud SET estado = 'Programada' WHERE id_solicitud = 1;

-- Asignacion de recursos a la orden
INSERT INTO asignacion_personal (id_orden, id_personal) VALUES
  (1, 1),   -- Carlos Gomez (piscinas)
  (1, 2);   -- Marta Diaz (paisajismo)

INSERT INTO asignacion_activo (id_orden, id_activo) VALUES
  (1, 1),   -- Camioneta
  (1, 2);   -- Cortadora


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

-- Consulta B: VALIDACION DE SOLAPAMIENTO (nucleo del RF007 / CU007).
-- Antes de asignar al personal 1 a una nueva orden el 2026-06-10 de 10:00 a 14:00,
-- se verifica si ya tiene una orden que se cruce en fecha y rango horario.
-- Si devuelve filas, la asignacion debe bloquearse.
SELECT ot.id_orden, ot.fecha_servicio, ot.hora_inicio, ot.hora_fin
FROM asignacion_personal ap
JOIN orden_trabajo ot ON ap.id_orden = ot.id_orden
WHERE ap.id_personal = 1
  AND ot.fecha_servicio = '2026-06-10'
  AND ot.estado = 'Asignada'
  AND ot.hora_inicio < '14:00:00'   -- se solapan si: inicio_existente < fin_nuevo
  AND ot.hora_fin    > '10:00:00';  --            y    fin_existente   > inicio_nuevo

-- Consulta C: Activos que NO estan disponibles para asignar (RF007 / RF009).
SELECT a.id_activo, a.descripcion, t.nombre AS tipo, a.estado
FROM activo a
JOIN tipo_activo t ON a.id_tipo = t.id_tipo
WHERE a.estado <> 'Disponible';


-- =====================================================================
-- 3) BORRADO DE DATOS DE PRUEBA Y VERIFICACION
-- =====================================================================
-- Se borra en orden inverso a la creacion para respetar las claves foraneas.

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
