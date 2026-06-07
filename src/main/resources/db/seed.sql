-- SCGO MVP: datos iniciales para ciclo CU006 -> CU007 -> CU008.
-- Requiere esquema creado (db/schema.sql). Una solicitud PENDIENTE sin orden previa.

INSERT INTO especialidad (nombre) VALUES
  ('Plomeria'), ('Electricidad'), ('Gas'), ('Piscinas'), ('Paisajismo');

INSERT INTO tipo_activo (nombre) VALUES
  ('Vehiculo'), ('Maquinaria');

INSERT INTO cliente (razon_social, tipo, telefono, email) VALUES
  ('Consorcio Torres del Parque', 'B2B', '381-4001122', 'admin@torresparque.com'),
  ('Juan Perez', 'B2C', '381-5559876', 'jperez@mail.com');

INSERT INTO propiedad (id_cliente, direccion, caracteristicas) VALUES
  (1, 'Av. Mate de Luna 1500, Tucuman', 'Predio con 2 piscinas y parque'),
  (1, 'Calle San Lorenzo 250, Tucuman', 'Sala de maquinas, grupo electrogeno'),
  (2, 'Bv. Aconquija 880, Yerba Buena', 'Vivienda con jardin');

INSERT INTO personal (id_especialidad, nombre, estado) VALUES
  (4, 'Carlos Gomez', 'Activo'),
  (5, 'Marta Diaz', 'Activo'),
  (2, 'Luis Fernandez', 'Licencia');

INSERT INTO activo (id_tipo, descripcion, estado) VALUES
  (1, 'Camioneta Hilux AB123CD', 'Disponible'),
  (2, 'Cortadora de cesped industrial', 'Disponible'),
  (2, 'Bomba sumergible para piscina', 'En Reparacion');

INSERT INTO solicitud (id_cliente, id_propiedad, fecha_ingreso, descripcion, estado) VALUES
  (1, 1, '2026-06-05', 'Mantenimiento mensual de piscinas y parque', 'Pendiente');
