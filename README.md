# SCGO — ServiHome

**SCGO** (Sistema Centralizado de Gestión Operativa) es el MVP interno de **ServiHome** para gestionar el ciclo de vida de las órdenes de trabajo: desde una solicitud de servicio hasta el cierre con registro de horas, materiales y averías.

## Alcance del MVP

El sistema implementa tres casos de uso encadenados:

| Caso de uso | Descripción |
|-------------|-------------|
| **CU006** | Generar orden de trabajo a partir de una solicitud `PENDIENTE` |
| **CU007** | Asignar personal o activos validando disponibilidad y solapamiento horario |
| **CU008** | Cerrar la orden de forma transaccional (cierre + estado `FINALIZADA`) |

## Stack tecnológico

- **Java 17**
- **MySQL 8** (motor InnoDB)
- **JDBC** con MySQL Connector/J (`PreparedStatement`)
- Patrón **MVC + DAO**
- `java.time` para fechas/horas, `BigDecimal` para decimales
- **JUnit 5** para pruebas

## Arquitectura

```
Vista (MenuConsola / GestionOrdenView)
    ↓
Controlador (OrdenController)
    ↓
DAO (JDBC)
    ↓
MySQL
```

Regla estricta: el controlador **no contiene SQL**; toda la persistencia vive en la capa DAO.

### Paquetes principales

```
com.servihome.scgo
├── config/       DatabaseConfig, DatabaseBootstrap
├── controlador/  OrdenController
├── dao/          Acceso JDBC
├── enums/        Estados del dominio
├── modelo/       POJOs / entidades
└── vista/        MenuConsola, GestionOrdenView
```

## Requisitos previos

- JDK 17 o superior
- MySQL 8.x accesible (local o Cloud; **no se asume localhost**)
- Maven 3.8+ (opcional, recomendado)

La base de datos debe llamarse `servihome` (o la que configures en `jdbc.database`). Al arrancar, la aplicación crea el esquema y carga datos demo si la BD está vacía.

## Configuración

### 1. Conexión JDBC

Copiá la plantilla y completá tus credenciales:

```bash
cp src/main/resources/database.properties.example src/main/resources/database.properties
```

Editá `database.properties`:

```properties
jdbc.host=tu-host-mysql.cloud
jdbc.port=3306
jdbc.database=servihome
jdbc.user=tu_usuario
jdbc.password=tu_clave
```

> `database.properties` está en `.gitignore` para no commitear credenciales.

**Alternativa:** apuntar a un archivo externo con la variable de entorno:

```bash
export SCGO_DB_CONFIG=/ruta/a/database.properties
```

### 2. Bootstrap automático

Al iniciar, `DatabaseBootstrap`:

1. Verifica la conexión JDBC.
2. Crea las tablas si no existen (`src/main/resources/db/schema.sql`).
3. Inserta datos demo si no hay clientes (`src/main/resources/db/seed.sql`).

**Datos demo incluidos:**

| Entidad | IDs relevantes | Notas |
|---------|----------------|-------|
| Solicitud | `1` | Estado `PENDIENTE` — lista para CU006 |
| Personal | `1` Carlos, `2` Marta | Operativos |
| Personal | `3` Luis | En licencia (rechazado en CU007) |
| Activos | `1` Camioneta, `2` Cortadora | Disponibles |
| Activo | `3` Bomba | En reparación (rechazado en CU007) |

Si la BD ya tiene datos cargados manualmente (por ejemplo, con scripts SQL propios), el seed **no** se vuelve a insertar. Usá la opción **4** del menú para consultar el estado actual.

## Compilación y ejecución

### Con Maven

```bash
mvn compile
mvn exec:java
```

### Manual

```bash
mkdir -p target/classes
javac --release 17 -cp lib/mysql-connector-j-8.4.0.jar \
  -d target/classes $(find src/main/java -name "*.java")

java -cp target/classes:lib/mysql-connector-j-8.4.0.jar com.servihome.scgo.Main
```

## Menú interactivo

Al ejecutar la aplicación aparece el menú principal:

```
========== SCGO - Menu principal ==========
  1. Generar orden de trabajo      (CU006)
  2. Asignar recurso a orden       (CU007)
  3. Cerrar orden de trabajo       (CU008)
  4. Consultar estado del sistema
  5. Ciclo completo guiado         (CU006->007->008)
  0. Salir
===========================================
```

### Opción 4 — Consultar estado

Muestra solicitudes pendientes, órdenes asignadas, personal operativo y activos disponibles. Útil para conocer los IDs antes de operar.

### Opción 5 — Ciclo completo guiado (recomendado)

Recorre CU006 → CU007 → CU008 en una sola sesión. Ejemplo con datos demo:

| Paso | Acción | Valores sugeridos |
|------|--------|-------------------|
| 1 — CU006 | Seleccionar solicitud y programar franja | Solicitud `1`, fecha `2026-06-10`, horario `08:00` – `12:00` |
| 2 — CU007 | Asignar personal | ID `1` (Carlos Gómez) |
| 2 — CU007 | Asignar activo | ID `1` (Camioneta Hilux) |
| 3 — CU008 | Registrar cierre | Horas `4.0`, materiales opcional, avería `n` |

Al finalizar deberías ver: **"Ciclo completo exitoso. Orden X FINALIZADA."**

### Uso paso a paso (opciones individuales)

#### CU006 — Generar orden (opción 1)

1. Elegí **1** en el menú.
2. Ingresá el **ID de solicitud** (ej. `1`).
3. Completá la programación:
   - Fecha de servicio: `AAAA-MM-DD` (ej. `2026-06-10`)
   - Hora inicio / fin: `HH:MM` (ej. `08:00` y `12:00`; debe cumplir `horaFin > horaInicio`)

**Resultado:** se crea una orden en estado `ASIGNADA` y la solicitud pasa a `PROGRAMADA`.

#### CU007 — Asignar recurso (opción 2)

1. Elegí **2** en el menú.
2. Ingresá **ID de orden**, **ID de recurso** y **tipo** (`1` = Personal, `2` = Activo).

**Validaciones aplicadas:**

- El recurso debe estar operativo (personal `ACTIVO`, activo `DISPONIBLE`).
- No puede haber solapamiento horario con otra orden `ASIGNADA` del mismo recurso.
- Turnos contiguos (ej. uno termina 12:00 y otro empieza 12:00) **no** se consideran solapados.

**Resultado:** mensaje de éxito o *"Asignacion rechazada: recurso no operativo o solapamiento detectado"*.

#### CU008 — Cerrar orden (opción 3)

1. Elegí **3** en el menú.
2. Ingresá **ID de orden** y los datos del cierre:
   - Horas reales (decimal, ej. `4.5`)
   - Materiales (opcional)
   - Avería (`s`/`n`); si hay avería, el detalle es obligatorio

**Resultado:** la orden pasa a `FINALIZADA` y se persiste el cierre en una sola transacción (todo o nada).

## Reglas de negocio críticas

| Regla | Descripción |
|-------|-------------|
| **R1** | Solapamiento: `ini_existente < fin_nuevo AND fin_existente > ini_nuevo` (desigualdad estricta) |
| **R2** | Solo recursos operativos pueden asignarse |
| **R3** | `horaFin > horaInicio` en toda orden |
| **R4** | Cierre transaccional: insertar cierre + actualizar orden en una sola transacción |
| **R5** | Si `averia = true`, `detalleAveria` es obligatorio y `estadoDevolucion` debe ser `AVERIADO` |

## Pruebas

```bash
mvn test
```

| Test | Tipo | Descripción |
|------|------|-------------|
| `SolapamientoR1LogicaTest` | Unitario | Tabla de verdad R1 (8 casos de borde) — no requiere BD |
| `MenuConsolaTest` | Unitario | Verifica salida del menú |
| `AsignacionDAOSolapamientoTest` | Integración | Solapamiento contra MySQL (requiere `database.properties`) |
| `CicloCompletoIntegrationTest` | Integración | CU006 → CU007 → CU008 (requiere MySQL) |

Los tests de integración se omiten automáticamente si no hay conexión JDBC configurada.

## Solución de problemas

| Problema | Causa probable | Qué hacer |
|----------|----------------|-----------|
| Error al conectar JDBC | `database.properties` ausente o credenciales incorrectas | Verificar host, puerto, usuario y clave |
| "No hay solicitudes PENDIENTES" | La BD ya tiene solicitudes programadas | Opción 4 para consultar, o vaciar tablas para recargar seed |
| Asignación rechazada | Recurso no operativo o solapamiento | Probar personal `1`/`2`, activos `1`/`2`; revisar horarios |
| `horaFin` inválido | R3 incumplida | Usar hora fin posterior a hora inicio |

## Fuera de alcance (MVP)

- Autenticación / RBAC
- Reportes gerenciales
- Reprogramación automática
- Notificaciones (email, WhatsApp)

## Licencia

Proyecto académico / interno ServiHome.
