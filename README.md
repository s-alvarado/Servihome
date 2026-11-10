# SCGO — ServiHome

**SCGO** (Sistema Centralizado de Gestión Operativa) es el MVP interno de **ServiHome** para gestionar el ciclo de vida de las órdenes de trabajo: desde una solicitud de servicio hasta el cierre con registro de horas, materiales y averías.

## Alcance del MVP

El sistema implementa tres casos de uso encadenados:

| Caso de uso | Descripción |
|-------------|-------------|
| **CU006** | Generar orden de trabajo a partir de una solicitud `PENDIENTE` |
| **CU007** | Asignar personal o activos validando disponibilidad y solapamiento horario |
| **CU008** | Cerrar la orden de forma transaccional (cierre + estado `FINALIZADA`). Si hay avería, los activos indicados pasan a `EN_REPARACION` en la misma transacción |

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
├── excepcion/    SolapamientoException
├── modelo/       POJOs / entidades
├── registro/     RegistroOperaciones, RegistroArchivo
└── vista/        MenuConsola, GestionOrdenView
```

## Requisitos previos

- JDK 17 o superior
- MySQL 8.x accesible (local o Cloud; **no se asume localhost**)
- Maven 3.8+ (opcional, recomendado)

La base de datos debe llamarse `servihome` (o la que configures en `jdbc.database`). Al arrancar, la aplicación crea las tablas si no existen y carga datos demo si no hay clientes.

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

## Scripts SQL

Al arrancar, la aplicación usa `src/main/resources/db/schema.sql` y, si no hay clientes, `seed.sql`. Los dos scripts de `sql/` se corren a mano.

`sql/01_creacion_tablas.sql` hace `DROP DATABASE IF EXISTS servihome`, vuelve a crear la base y define las tablas. `cliente.activo` (BOOLEAN, default `TRUE`) es la columna de la baja lógica (RF001). El esquema de la aplicación tiene la misma columna.

`sql/02_insercion_consulta_borrado.sql` inserta un juego de prueba, muestra consultas y, al inicio de la sección 3, deja el ejemplo de **baja lógica**:

```sql
UPDATE cliente SET activo = FALSE WHERE id_cliente = 2;
SELECT id_cliente, razon_social, activo FROM cliente;
```

Ese `id_cliente = 2` es Juan Pérez. El `UPDATE` deja la fila, con `activo = 0`. En este script la solicitud y la orden son del cliente 1 (Consorcio Torres del Parque); Juan Pérez queda referenciado por su propiedad. Los `DELETE` que siguen limpian todo el juego de prueba, en orden inverso a las claves foráneas, y ese cliente también se borra. Al terminar, los `COUNT(*)` de verificación dan 0 y el próximo arranque de la aplicación puede volver a cargar el seed.

La sección 1, corrida sola, deja la solicitud 1 en `Programada` y ya con una orden. CU006 del menú exige una solicitud `PENDIENTE`.

## Compilación y ejecución

### Con Maven

```bash
mvn compile
mvn exec:java
```

### Manual

`lib/` no se versiona. Para compilar sin Maven, copiá ahí el MySQL Connector/J `8.4.0` (el mismo artefacto que declara el `pom.xml`).

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
  6. Ver registro de operaciones (ultimas 10)
  0. Salir
===========================================
```

### Opción 4 — Consultar estado

Muestra solicitudes pendientes, órdenes asignadas, la lista polimórfica de recursos operativos, el personal operativo, los activos disponibles y un resumen de órdenes por estado (`ASIGNADA`, `FINALIZADA`, `CANCELADA`). Útil para conocer los IDs antes de operar.

### Opción 6 — Registro de operaciones

Muestra las últimas 10 líneas del archivo de operaciones, o `(sin registros)` si todavía no hay ninguna.

### Opción 5 — Ciclo completo guiado (recomendado)

Recorre CU006 → CU007 → CU008 en una sola sesión. Ejemplo con datos demo:

| Paso | Acción | Valores sugeridos |
|------|--------|-------------------|
| 1 — CU006 | Seleccionar solicitud y programar franja | Solicitud `1`, fecha `2026-06-10`, horario `08:00` – `12:00` |
| 2 — CU007 | Asignar personal | ID `1` (Carlos Gómez) |
| 2 — CU007 | Asignar activo | ID `1` (Camioneta Hilux) |
| 3 — CU008 | Registrar cierre | Horas `4.0`, materiales opcional, avería `n` (si es `s`, indicar el activo `1`) |

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

**Resultado:** `Recurso asignado correctamente a la orden X`. Si el recurso no está operativo: `Asignacion rechazada: Recurso no operativo: id=X`. Si hay solapamiento: `Solapamiento detectado:` y el motivo (recurso, fecha y franja).

#### CU008 — Cerrar orden (opción 3)

1. Elegí **3** en el menú.
2. Ingresá **ID de orden** y los datos del cierre:
   - Horas reales (decimal, ej. `4.5`)
   - Materiales (opcional)
   - Avería (`s`/`n`); si hay avería, el detalle es obligatorio
3. Si hay avería, el sistema muestra los activos asignados a esa orden. Indicá uno o más IDs (solo de esa lista, sin repetir) y `0` para terminar.

**Resultado:** la orden pasa a `FINALIZADA` y se persiste el cierre en una sola transacción (todo o nada). Cada activo averiado queda en `EN_REPARACION`. El mensaje final lista esos IDs.

## Reglas de negocio críticas

| Regla | Descripción |
|-------|-------------|
| **R1** | Solapamiento: `ini_existente < fin_nuevo AND fin_existente > ini_nuevo` (desigualdad estricta) |
| **R2** | Solo recursos operativos pueden asignarse |
| **R3** | `horaFin > horaInicio` en toda orden |
| **R4** | Cierre transaccional: insertar cierre + actualizar orden en una sola transacción |
| **R5** | Si `averia = true`, `detalleAveria` es obligatorio y `estadoDevolucion` debe ser `AVERIADO` |
| **R6** | Con avería, al menos un activo y todos asignados a la orden; sin avería, el arreglo de activos queda vacío. El cambio de estado de esos activos entra en la misma transacción del cierre |

## Registro de operaciones

Cada caso de uso deja una línea en un archivo **después** de confirmar la operación (o, si la transacción hace rollback, una línea `ERROR_SISTEMA`). Un fallo al escribir el archivo no interrumpe el negocio: solo se avisa por la salida de error.

| Evento | Cuándo |
|--------|--------|
| `ORDEN_GENERADA` | CU006 confirmado |
| `RECURSO_ASIGNADO` | CU007 confirmado |
| `ASIGNACION_RECHAZADA` | Recurso no operativo o solapamiento, antes de lanzar la excepción |
| `ORDEN_CERRADA` | CU008 confirmado |
| `AVERIA_REPORTADA` | Una línea por activo averiado, después del cierre |
| `ERROR_SISTEMA` | Fallo transaccional, ya hecho el rollback |

Formato: `2026-11-09T19:30:12 | ORDEN_CERRADA | orden=5 horas=4.0`

Las horas salen de `BigDecimal.toPlainString()`, así que un `4.0` ingresado queda `horas=4.0`.

Por defecto el archivo es `logs/scgo.log`. Otra ruta: `-Dscgo.log.path=/ruta/scgo.log`. La carpeta `logs/` no se versiona.

## Arreglos y ArrayList

Las listas que salen de la base son `ArrayList` (el tamaño depende de las filas). Los arreglos se usan cuando el tamaño ya se conoce:

| Uso | Dónde |
|-----|--------|
| `String[]` con las opciones del menú | `MenuConsola` |
| `int[]` de órdenes por estado, indexado con `EstadoOrden.ordinal()` | `OrdenController.contarPorEstado`, mostrado en la opción 4 |
| `ArrayList<Integer>` al capturar averías y `int[]` al cerrar la orden | `GestionOrdenView.capturarYRegistrarCierre` y `OrdenController.cerrarOrden` |

## Pruebas

```bash
mvn test
```

| Test | Tipo | Descripción |
|------|------|-------------|
| `SolapamientoR1LogicaTest` | Unitario | Tabla de verdad R1 (8 casos de borde) — no requiere BD |
| `RecursoPolimorfismoTest` | Unitario | `estaDisponible()` sobre el tipo `Recurso` (personal activo y en licencia) — no requiere BD |
| `MenuConsolaTest` | Unitario | Verifica salida del menú, incluida la opción 6 |
| `ContarPorEstadoTest` | Unitario | Contadores por `EstadoOrden` sin base de datos |
| `RegistroArchivoTest` | Unitario | Escritura y lectura del archivo (`@TempDir`) |
| `AsignacionDAOSolapamientoTest` | Integración | Solapamiento contra MySQL (requiere `database.properties`) |
| `CicloCompletoIntegrationTest` | Integración | CU006 → CU007 → CU008 (requiere MySQL) |
| `CierreConAveriaIntegrationTest` | Integración | CPS04: cierre con avería y rechazo R6 |
| `TransaccionRollbackIntegrationTest` | Integración | CPI05: un fallo en el segundo paso del cierre no deja datos parciales |

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
