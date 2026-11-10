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

- **JDK 17.** El `pom.xml` fija `maven.compiler.release` en 17, así que el bytecode es el de Java 17. Maven tiene que arrancar con ese JDK en `JAVA_HOME`: mira esa variable, no el `java` del `PATH`.
- MySQL 8.x accesible (local o Cloud; **no se asume localhost**)
- Maven 3.8+ (opcional, recomendado)

Si `JAVA_HOME` queda en Java 11, el build corta al inicio: el enforcer de Maven exige Java 17 o posterior. Sin eso, `mvn exec:java` llega a cargar clases 17 (`class file version 61.0`) con un runtime que solo reconoce hasta la `55.0`.

En Ubuntu, si el JDK 17 no está instalado:

```bash
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version
```

La línea `Java version` tiene que ser 17. Un JDK más nuevo también compila este release; uno anterior, no.

La base indicada en `jdbc.database` tiene que existir antes de arrancar. La aplicación crea las tablas si no existen y carga datos demo si no hay clientes; no crea la base.

## Configuración

### 1. Conexión JDBC

Copiá la plantilla y reemplazá host, puerto, usuario y clave por los de tu MySQL. Los valores que trae el archivo son un ejemplo local (`127.0.0.1`, puerto `3307`, usuario `root`); si tu servidor escucha en otro puerto (el de MySQL suele ser `3306`), cambialos. No hace falta que el host sea un servidor en la nube.

```bash
cp src/main/resources/database.properties.example src/main/resources/database.properties
```

Si la base todavía no existe:

```sql
CREATE DATABASE IF NOT EXISTS servihome
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

La plantilla ya trae esas claves. Si tu MySQL no es el del ejemplo, editá solo estos valores:

```properties
jdbc.host=127.0.0.1
jdbc.port=3307
jdbc.database=servihome
jdbc.user=root
jdbc.password=scgo-local
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
| Personal | `1` Carlos Gomez, `2` Marta Diaz | Operativos (`ACTIVO`) |
| Personal | `3` Luis Fernandez | En licencia (rechazado en CU007) |
| Activos | `1` Camioneta Hilux AB123CD, `2` Cortadora de cesped industrial | Disponibles |
| Activo | `3` Bomba sumergible para piscina | En reparación (rechazado en CU007) |

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

`mvn -version` tiene que mostrar Java 17. Si no, apuntá `JAVA_HOME` al JDK 17 y recompilá desde cero (`clean` descarta clases generadas con otro runtime).

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version

mvn clean compile
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

La opción **5** recorre CU006, CU007 y CU008 sin volver al menú. Con los datos demo, la consola lista lo pendiente y pide el id antes del paso 1:

```
[SCGO]   id=1 | Mantenimiento mensual de piscinas y parque | ingreso=2026-06-05 | estado=PENDIENTE
[SCGO] Seleccione ID de solicitud:
1
```

**Paso 1.** Pide la franja, no vuelve a pedir la solicitud. Al confirmar muestra la orden y la deja como orden activa del ciclo.

```
[SCGO] --- Paso 1/3: CU006 Generar orden ---
[SCGO] Programacion de la orden de servicio:
[SCGO] Fecha de servicio (AAAA-MM-DD):
2026-06-10
[SCGO] Hora inicio (HH:MM):
08:00
[SCGO] Hora fin (HH:MM):
12:00
[SCGO] Orden generada: id=1, estado=ASIGNADA, franja=2026-06-10 08:00-12:00
[SCGO] Orden activa para el ciclo: id=1
```

**Paso 2.** Lista el personal operativo y, después de asignarlo, los activos disponibles. El id se elige de la lista que acaba de imprimir.

```
[SCGO] --- Paso 2/3: CU007 Asignar recursos ---
[SCGO]   id=1 | Carlos Gomez | ACTIVO
[SCGO]   id=2 | Marta Diaz | ACTIVO
[SCGO] ID de personal a asignar:
1
[SCGO] Recurso asignado correctamente a la orden 1
[SCGO]   id=1 | Camioneta Hilux AB123CD | DISPONIBLE
[SCGO]   id=2 | Cortadora de cesped industrial | DISPONIBLE
[SCGO] ID de activo a asignar:
1
[SCGO] Recurso asignado correctamente a la orden 1
```

**Paso 3.** Pide horas, materiales (Enter los omite) y si hubo avería. Con `s`, el detalle es obligatorio. Después lista solo los activos de esa orden y pide ids hasta que se ingresa `0`. Un id repetido no se vuelve a tomar: avisa `El activo id=1 ya fue indicado.` y repregunta.

```
[SCGO] --- Paso 3/3: CU008 Cerrar orden ---
[SCGO] Horas reales (ej. 4.5):
4.0
[SCGO] Materiales utilizados (Enter para omitir):
[SCGO] Hubo averia en algun activo? (s/n):
s
[SCGO] Detalle de la averia:
Falla bomba nafta
[SCGO] --- Activos asignados a la orden 1 ---
[SCGO]   id=1 | Camioneta Hilux AB123CD | DISPONIBLE
[SCGO] ID de activo averiado, 0 para terminar:
1
[SCGO] ID de activo averiado, 0 para terminar:
0
[SCGO] Orden 1 cerrada correctamente (FINALIZADA). Activos enviados a reparacion: [1]
[SCGO] Ciclo completo exitoso. Orden 1 FINALIZADA.
```

Con avería `n` no pide detalle ni ids de activos. El cierre queda en `Orden 1 cerrada correctamente (FINALIZADA)` y la línea de éxito del ciclo es la misma.

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
| Error al conectar JDBC | `database.properties` ausente, credenciales incorrectas o la base no existe | Verificar host, puerto, usuario y clave; crear la base si falta |
| El enforcer pide Java 17, o `class file version 61.0` / `up to 55.0` | `JAVA_HOME` apunta a un JDK anterior a 17 | Instalar JDK 17, exportar `JAVA_HOME` y correr `mvn clean compile` |
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
