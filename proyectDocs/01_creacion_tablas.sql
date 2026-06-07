-- =====================================================================
-- Sistema Centralizado de Gestion Operativa (SCGO) - ServiHome
-- Script 01: Creacion de la base de datos y tablas
-- Motor: MySQL 8.x  /  Codificacion: utf8mb4  /  Engine: InnoDB (soporte FK + ACID)
-- Autor: Alvarado Santiago Ignacio - Legajo VINF011046
-- =====================================================================

DROP DATABASE IF EXISTS servihome;
CREATE DATABASE servihome
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE servihome;

-- ---------------------------------------------------------------------
-- Tablas de catalogo (3FN: se extraen para evitar redundancia textual)
-- ---------------------------------------------------------------------

CREATE TABLE especialidad (
    id_especialidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE tipo_activo (
    id_tipo INT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(40) NOT NULL UNIQUE   -- 'Vehiculo' / 'Maquinaria'
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Entidades principales
-- ---------------------------------------------------------------------

CREATE TABLE cliente (
    id_cliente    INT AUTO_INCREMENT PRIMARY KEY,
    razon_social  VARCHAR(120) NOT NULL,
    tipo          ENUM('B2B','B2C') NOT NULL,
    telefono      VARCHAR(30),
    email         VARCHAR(120),
    activo        BOOLEAN NOT NULL DEFAULT TRUE   -- baja logica (RF001)
) ENGINE=InnoDB;

CREATE TABLE propiedad (
    id_propiedad     INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente       INT NOT NULL,
    direccion        VARCHAR(160) NOT NULL,
    caracteristicas  VARCHAR(255),
    CONSTRAINT fk_propiedad_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
) ENGINE=InnoDB;

CREATE TABLE personal (
    id_personal      INT AUTO_INCREMENT PRIMARY KEY,
    id_especialidad  INT NOT NULL,
    nombre           VARCHAR(120) NOT NULL,
    estado           ENUM('Activo','Licencia','Enfermedad') NOT NULL DEFAULT 'Activo',
    CONSTRAINT fk_personal_especialidad
        FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad)
) ENGINE=InnoDB;

CREATE TABLE activo (
    id_activo    INT AUTO_INCREMENT PRIMARY KEY,
    id_tipo      INT NOT NULL,
    descripcion  VARCHAR(120) NOT NULL,
    estado       ENUM('Disponible','En Reparacion','Baja') NOT NULL DEFAULT 'Disponible',
    CONSTRAINT fk_activo_tipo
        FOREIGN KEY (id_tipo) REFERENCES tipo_activo(id_tipo)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Tablas transaccionales
-- ---------------------------------------------------------------------

CREATE TABLE solicitud (
    id_solicitud  INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente    INT NOT NULL,
    id_propiedad  INT NOT NULL,
    fecha_ingreso DATE NOT NULL,
    descripcion   VARCHAR(255) NOT NULL,
    estado        ENUM('Pendiente','Programada','Reprogramacion') NOT NULL DEFAULT 'Pendiente',
    CONSTRAINT fk_solicitud_cliente
        FOREIGN KEY (id_cliente)   REFERENCES cliente(id_cliente),
    CONSTRAINT fk_solicitud_propiedad
        FOREIGN KEY (id_propiedad) REFERENCES propiedad(id_propiedad)
) ENGINE=InnoDB;

CREATE TABLE orden_trabajo (
    id_orden       INT AUTO_INCREMENT PRIMARY KEY,
    id_solicitud   INT NOT NULL,
    fecha_servicio DATE NOT NULL,
    hora_inicio    TIME NOT NULL,
    hora_fin       TIME NOT NULL,
    estado         ENUM('Asignada','Finalizada','Cancelada') NOT NULL DEFAULT 'Asignada',
    CONSTRAINT fk_orden_solicitud
        FOREIGN KEY (id_solicitud) REFERENCES solicitud(id_solicitud),
    CONSTRAINT chk_rango_horario CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB;

-- Tablas de asignacion: resuelven el N:M Orden <-> Recurso.
-- Sobre estas tablas opera la validacion de solapamiento (RF007).

CREATE TABLE asignacion_personal (
    id_asig_pers INT AUTO_INCREMENT PRIMARY KEY,
    id_orden     INT NOT NULL,
    id_personal  INT NOT NULL,
    CONSTRAINT fk_asigpers_orden
        FOREIGN KEY (id_orden)    REFERENCES orden_trabajo(id_orden),
    CONSTRAINT fk_asigpers_personal
        FOREIGN KEY (id_personal) REFERENCES personal(id_personal),
    CONSTRAINT uq_asigpers UNIQUE (id_orden, id_personal)
) ENGINE=InnoDB;

CREATE TABLE asignacion_activo (
    id_asig_act INT AUTO_INCREMENT PRIMARY KEY,
    id_orden    INT NOT NULL,
    id_activo   INT NOT NULL,
    CONSTRAINT fk_asigact_orden
        FOREIGN KEY (id_orden)  REFERENCES orden_trabajo(id_orden),
    CONSTRAINT fk_asigact_activo
        FOREIGN KEY (id_activo) REFERENCES activo(id_activo),
    CONSTRAINT uq_asigact UNIQUE (id_orden, id_activo)
) ENGINE=InnoDB;

CREATE TABLE cierre_orden (
    id_cierre         INT AUTO_INCREMENT PRIMARY KEY,
    id_orden          INT NOT NULL UNIQUE,        -- 1:1 con la orden
    horas_reales      DECIMAL(5,2) NOT NULL,
    materiales        VARCHAR(255),
    estado_devolucion ENUM('Buen Estado','Averiado') NOT NULL DEFAULT 'Buen Estado',
    averia            BOOLEAN NOT NULL DEFAULT FALSE,
    detalle_averia    VARCHAR(255),
    CONSTRAINT fk_cierre_orden
        FOREIGN KEY (id_orden) REFERENCES orden_trabajo(id_orden)
) ENGINE=InnoDB;
