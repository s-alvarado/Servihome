CREATE TABLE IF NOT EXISTS especialidad (
    id_especialidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tipo_activo (
    id_tipo INT AUTO_INCREMENT PRIMARY KEY,
    nombre  VARCHAR(40) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cliente (
    id_cliente    INT AUTO_INCREMENT PRIMARY KEY,
    razon_social  VARCHAR(120) NOT NULL,
    tipo          ENUM('B2B','B2C') NOT NULL,
    telefono      VARCHAR(30),
    email         VARCHAR(120),
    activo        BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS propiedad (
    id_propiedad     INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente       INT NOT NULL,
    direccion        VARCHAR(160) NOT NULL,
    caracteristicas  VARCHAR(255),
    CONSTRAINT fk_propiedad_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS personal (
    id_personal      INT AUTO_INCREMENT PRIMARY KEY,
    id_especialidad  INT NOT NULL,
    nombre           VARCHAR(120) NOT NULL,
    estado           ENUM('Activo','Licencia','Enfermedad') NOT NULL DEFAULT 'Activo',
    CONSTRAINT fk_personal_especialidad
        FOREIGN KEY (id_especialidad) REFERENCES especialidad(id_especialidad)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS activo (
    id_activo    INT AUTO_INCREMENT PRIMARY KEY,
    id_tipo      INT NOT NULL,
    descripcion  VARCHAR(120) NOT NULL,
    estado       ENUM('Disponible','En Reparacion','Baja') NOT NULL DEFAULT 'Disponible',
    CONSTRAINT fk_activo_tipo
        FOREIGN KEY (id_tipo) REFERENCES tipo_activo(id_tipo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS solicitud (
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

CREATE TABLE IF NOT EXISTS orden_trabajo (
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

CREATE TABLE IF NOT EXISTS asignacion_personal (
    id_asig_pers INT AUTO_INCREMENT PRIMARY KEY,
    id_orden     INT NOT NULL,
    id_personal  INT NOT NULL,
    CONSTRAINT fk_asigpers_orden
        FOREIGN KEY (id_orden)    REFERENCES orden_trabajo(id_orden),
    CONSTRAINT fk_asigpers_personal
        FOREIGN KEY (id_personal) REFERENCES personal(id_personal),
    CONSTRAINT uq_asigpers UNIQUE (id_orden, id_personal)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS asignacion_activo (
    id_asig_act INT AUTO_INCREMENT PRIMARY KEY,
    id_orden    INT NOT NULL,
    id_activo   INT NOT NULL,
    CONSTRAINT fk_asigact_orden
        FOREIGN KEY (id_orden)  REFERENCES orden_trabajo(id_orden),
    CONSTRAINT fk_asigact_activo
        FOREIGN KEY (id_activo) REFERENCES activo(id_activo),
    CONSTRAINT uq_asigact UNIQUE (id_orden, id_activo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cierre_orden (
    id_cierre         INT AUTO_INCREMENT PRIMARY KEY,
    id_orden          INT NOT NULL UNIQUE,
    horas_reales      DECIMAL(5,2) NOT NULL,
    materiales        VARCHAR(255),
    estado_devolucion ENUM('Buen Estado','Averiado') NOT NULL DEFAULT 'Buen Estado',
    averia            BOOLEAN NOT NULL DEFAULT FALSE,
    detalle_averia    VARCHAR(255),
    CONSTRAINT fk_cierre_orden
        FOREIGN KEY (id_orden) REFERENCES orden_trabajo(id_orden)
) ENGINE=InnoDB;
