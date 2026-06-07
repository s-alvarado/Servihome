# SCGO — ServiHome · Briefing técnico para desarrollo del MVP

> **Propósito de este documento.** Es el contexto de arranque para un agente de IA que va a escribir el código del MVP. Condensa las especificaciones accionables del proyecto (entidades, capas, contratos, reglas de negocio, esquema de datos y pruebas). No es un informe académico: cada sección está pensada para traducirse directamente a código. Donde haya que decidir algo no especificado aquí, seguir las convenciones marcadas en "Convenciones" y dejar un `// TODO:` explícito.

---

## 1. Qué se construye

**SCGO** (Sistema Centralizado de Gestión Operativa) es el sistema interno de **ServiHome**, una empresa de mantenimiento del hogar (plomería, electricidad, gas, piscinas, paisajismo; 25 empleados; clientes B2B y B2C). El sistema reemplaza la gestión manual (Excel, WhatsApp, pizarras) y centraliza el ciclo de vida de las órdenes de trabajo.

**Alcance del MVP:** el ciclo de vida completo de la orden de trabajo, a través de tres casos de uso encadenados:

- **CU006 — Generar orden de trabajo:** a partir de una solicitud existente se crea una orden y la solicitud pasa a `Programada`.
- **CU007 — Asignar recursos:** se asignan personal y/o activos a la orden, **validando que no haya solapamiento horario** ni recursos no operativos. Es la lógica crítica del sistema.
- **CU008 — Cierre de orden:** se registra el cierre (horas reales, materiales, estado de devolución, avería) y la orden pasa a `Finalizada`. Operación transaccional.

**Problemas del negocio que el MVP debe resolver:**
1. Solapamiento de recursos en la planificación → lo ataca la validación del CU007.
2. "Daños fantasma" (herramientas que vuelven dañadas sin registro) → lo ataca el registro de avería en el cierre (CU008).
3. Falta de trazabilidad y retroalimentación → lo ataca la persistencia estructurada de todo el ciclo.

---

## 2. Stack y arquitectura

| Aspecto | Decisión | Nota para el desarrollo |
|---|---|---|
| Lenguaje | Java | Usar `java.time` (`LocalDate`, `LocalTime`) y `BigDecimal`. |
| Persistencia | MySQL 8.x, motor **InnoDB** | InnoDB es obligatorio: el sistema depende de FK y de transacciones ACID. |
| Acceso a datos | **JDBC** (MySQL Connector/J) | Usar siempre `PreparedStatement` (nunca concatenar SQL). |
| Patrón | **MVC** + capa **DAO** | Vista → Controlador → DAO → BD. El controlador no arma SQL; eso vive en los DAO. |
| Seguridad | RBAC (control de acceso por roles) | Fuera del núcleo del MVP, pero no bloquear su incorporación. |
| Infraestructura | Cloud (app server + db server separados) | La conexión JDBC apunta a un host remoto, puerto 3306. No asumir `localhost`. |

**Regla de capas (estricta):**
- `Vista` solo habla con `Controlador`.
- `Controlador` orquesta la lógica de los CU y habla con los `DAO`. No contiene SQL.
- `DAO` encapsula todo el JDBC y devuelve/recibe objetos del Modelo.
- `Modelo` (entidades) son POJOs sin lógica de persistencia.

---

## 3. Modelo de dominio (entidades / POJOs)

Todas las entidades son POJOs con campos privados, getters/setters, y se mapean 1:1 con las tablas de la sección 7. Los estados se modelan como **enums** (sección 4).

### Cliente
| Campo | Tipo Java | Notas |
|---|---|---|
| idCliente | int | PK |
| razonSocial | String | |
| tipo | TipoCliente (enum) | B2B / B2C |
| telefono | String | nullable |
| email | String | nullable |
| activo | boolean | baja lógica (default true) |

### Propiedad
Un cliente tiene 0..N propiedades (relación 1:N — un consorcio B2B puede tener varios inmuebles).
| Campo | Tipo Java | Notas |
|---|---|---|
| idPropiedad | int | PK |
| idCliente | int | FK → Cliente |
| direccion | String | |
| caracteristicas | String | nullable |

### Personal
| Campo | Tipo Java | Notas |
|---|---|---|
| idPersonal | int | PK |
| idEspecialidad | int | FK → Especialidad |
| nombre | String | |
| estado | EstadoPersonal (enum) | Activo / Licencia / Enfermedad |
| **estaOperativo()** | boolean | regla: `estado == Activo` |

### Activo (recurso físico: vehículos, maquinaria)
| Campo | Tipo Java | Notas |
|---|---|---|
| idActivo | int | PK |
| idTipo | int | FK → TipoActivo |
| descripcion | String | |
| estado | EstadoActivo (enum) | Disponible / EnReparacion / Baja |
| **estaDisponible()** | boolean | regla: `estado == Disponible` |

### Solicitud
| Campo | Tipo Java | Notas |
|---|---|---|
| idSolicitud | int | PK |
| idCliente | int | FK → Cliente |
| idPropiedad | int | FK → Propiedad |
| fechaIngreso | LocalDate | |
| descripcion | String | |
| estado | EstadoSolicitud (enum) | Pendiente / Programada / Reprogramacion |

### OrdenTrabajo
| Campo | Tipo Java | Notas |
|---|---|---|
| idOrden | int | PK |
| idSolicitud | int | FK → Solicitud |
| fechaServicio | LocalDate | |
| horaInicio | LocalTime | |
| horaFin | LocalTime | invariante: `horaFin > horaInicio` |
| estado | EstadoOrden (enum) | Asignada / Finalizada / Cancelada |
| **getFranjaHoraria()** | Franja | objeto auxiliar {fecha, ini, fin} |

### CierreOrden
Relación 1:1 con OrdenTrabajo.
| Campo | Tipo Java | Notas |
|---|---|---|
| idCierre | int | PK |
| idOrden | int | FK → OrdenTrabajo, UNIQUE |
| horasReales | BigDecimal | |
| materiales | String | nullable |
| estadoDevolucion | EstadoDevolucion (enum) | BuenEstado / Averiado |
| averia | boolean | |
| detalleAveria | String | nullable; obligatorio si `averia == true` |

### Catálogos
- **Especialidad** { idEspecialidad:int (PK), nombre:String } — valores: Plomeria, Electricidad, Gas, Piscinas, Paisajismo.
- **TipoActivo** { idTipo:int (PK), nombre:String } — valores: Vehiculo, Maquinaria.

### Objeto auxiliar
- **Franja** { fecha:LocalDate, inicio:LocalTime, fin:LocalTime } — value object para pasar el rango horario a la validación de solapamiento.

---

## 4. Enumeraciones

```java
enum TipoCliente { B2B, B2C }
enum EstadoSolicitud { PENDIENTE, PROGRAMADA, REPROGRAMACION }
enum EstadoOrden { ASIGNADA, FINALIZADA, CANCELADA }
enum EstadoPersonal { ACTIVO, LICENCIA, ENFERMEDAD }
enum EstadoActivo { DISPONIBLE, EN_REPARACION, BAJA }
enum EstadoDevolucion { BUEN_ESTADO, AVERIADO }
enum TipoRecurso { PERSONAL, ACTIVO }   // discrimina qué se asigna en CU007
```

Mapeo enum ↔ columna `ENUM` de MySQL: mantener correspondencia explícita en el DAO (los valores de MySQL usan etiquetas legibles como `'En Reparacion'`; convertir en la capa DAO).

---

## 5. Capa de control (controlador)

### OrdenController `<<control>>`
Orquesta los tres casos de uso. Depende de los cuatro DAO por composición.

```
- ordenDAO     : OrdenTrabajoDAO
- solicitudDAO : SolicitudDAO
- asignacionDAO: AsignacionDAO
- cierreDAO    : CierreOrdenDAO
```

| Método | Firma | Lógica (qué debe hacer) |
|---|---|---|
| generarOrden | `OrdenTrabajo generarOrden(int idSolicitud)` | **CU006.** Buscar la solicitud; crear `OrdenTrabajo` (estado `ASIGNADA`) vía `ordenDAO.insertar`; cambiar la solicitud a `PROGRAMADA` vía `solicitudDAO.actualizarEstado`. Devuelve la orden creada. |
| validarYAsignar | `boolean validarYAsignar(int idOrden, int idRecurso, TipoRecurso tipo)` | **CU007.** Obtener la franja de la orden; verificar disponibilidad del recurso (operativo + sin solapamiento); si OK registrar la asignación y devolver `true`; si no, devolver `false` sin tocar la BD. |
| verificarSolapamiento | `boolean verificarSolapamiento(int idRecurso, TipoRecurso tipo, LocalDate fecha, LocalTime ini, LocalTime fin)` | Delega en `asignacionDAO.existeSolapamiento(...)`. Ver regla en sección 6. |
| cerrarOrden | `void cerrarOrden(int idOrden, CierreOrden cierre)` | **CU008.** **Transacción:** `setAutoCommit(false)` → `cierreDAO.insertar(cierre)` → `ordenDAO.actualizarEstado(idOrden, FINALIZADA)` → `commit()`. Si algo falla, `rollback()`. |

---

## 6. Reglas de negocio (críticas — verificadas)

### R1. Validación de solapamiento horario (núcleo del CU007 / RF007)
Antes de asignar un recurso a una orden, se comprueba si ese recurso ya está asignado a **otra orden** en la **misma fecha** cuyo rango horario **se cruza**. La condición de cruce entre `[ini_existente, fin_existente]` y `[ini_nuevo, fin_nuevo]` es:

```
ini_existente < fin_nuevo  AND  fin_existente > ini_nuevo
```

- Se usa **desigualdad estricta** a propósito: turnos contiguos (uno termina 12:00, otro empieza 12:00) **no** se consideran solapados — son válidos.
- Solo se consideran órdenes en estado `ASIGNADA` (no las canceladas).
- Si la consulta devuelve ≥1 fila → hay solapamiento → **bloquear** la asignación.

**Tabla de verdad verificada** (orden existente 08:00–12:00):

| Franja nueva | ¿Bloquea? |
|---|---|
| 06:00–07:59 | No |
| 06:00–08:00 (toca borde) | No |
| 06:00–08:01 | Sí |
| 09:00–11:00 (contenida) | Sí |
| 11:59–14:00 | Sí |
| 12:00–14:00 (toca borde) | No |
| 12:01–14:00 | No |
| 07:00–13:00 (contiene) | Sí |

### R2. Recurso operativo
Un recurso solo puede asignarse si está operativo: `Personal.estaOperativo()` (estado `ACTIVO`) o `Activo.estaDisponible()` (estado `DISPONIBLE`). Personal en licencia/enfermedad o activos en reparación/baja se rechazan.

### R3. Invariante de horario de la orden
`horaFin > horaInicio` siempre. Validar en la capa de aplicación **y** está reforzado por un `CHECK` en la BD.

### R4. Cierre transaccional
La inserción del cierre y el cambio de estado de la orden a `FINALIZADA` ocurren en una sola transacción (todo o nada). Es la justificación operativa de InnoDB.

### R5. Avería obligatoria con detalle
Si `CierreOrden.averia == true`, entonces `detalleAveria` no puede ser nulo/vacío y `estadoDevolucion` debería ser `AVERIADO`.

---

## 7. Esquema de base de datos

> Los scripts SQL completos y ejecutables se entregan aparte (`01_creacion_tablas.sql`, `02_insercion_consulta_borrado.sql`). Validados estructuralmente (orden de FK, balance de paréntesis) y la lógica de solapamiento verificada contra casos de prueba. Resumen del esquema:

**Base de datos:** `servihome` (utf8mb4). **11 tablas, todas InnoDB.**

```
especialidad(id_especialidad PK, nombre UNIQUE)
tipo_activo(id_tipo PK, nombre UNIQUE)
cliente(id_cliente PK, razon_social, tipo ENUM(B2B,B2C), telefono, email, activo)
propiedad(id_propiedad PK, id_cliente FK, direccion, caracteristicas)
personal(id_personal PK, id_especialidad FK, nombre, estado ENUM)
activo(id_activo PK, id_tipo FK, descripcion, estado ENUM)
solicitud(id_solicitud PK, id_cliente FK, id_propiedad FK, fecha_ingreso, descripcion, estado ENUM)
orden_trabajo(id_orden PK, id_solicitud FK, fecha_servicio, hora_inicio, hora_fin, estado ENUM,
              CHECK hora_fin > hora_inicio)
asignacion_personal(id_asig_pers PK, id_orden FK, id_personal FK, UNIQUE(id_orden,id_personal))
asignacion_activo(id_asig_act PK, id_orden FK, id_activo FK, UNIQUE(id_orden,id_activo))
cierre_orden(id_cierre PK, id_orden FK UNIQUE, horas_reales DECIMAL(5,2), materiales,
             estado_devolucion ENUM, averia BOOLEAN, detalle_averia)
```

**Por qué las tablas de asignación son dos y no una:** resuelven el N:M entre orden y recursos, y son la base física sobre la que corre la consulta de solapamiento (R1). El `UNIQUE(id_orden, id_recurso)` impide doble asignación del mismo recurso a la misma orden.

### Consulta de solapamiento (referencia para `AsignacionDAO.existeSolapamiento`)
```sql
SELECT ot.id_orden
FROM asignacion_personal ap
JOIN orden_trabajo ot ON ap.id_orden = ot.id_orden
WHERE ap.id_personal = ?      -- idRecurso
  AND ot.fecha_servicio = ?   -- fecha
  AND ot.estado = 'Asignada'
  AND ot.hora_inicio < ?      -- fin_nuevo
  AND ot.hora_fin    > ?;     -- ini_nuevo
-- Para activos: misma consulta sobre asignacion_activo / id_activo.
```

---

## 8. Capa de acceso a datos (DAO)

Interfaz genérica común:
```java
interface DAO<T> {
    int insertar(T obj);
    T   buscarPorId(int id);
    void actualizar(T obj);
}
```

| DAO | Métodos clave | Responsabilidad |
|---|---|---|
| SolicitudDAO | `Solicitud buscarPorId(int)`, `void actualizarEstado(int, EstadoSolicitud)` | Leer solicitud, cambiar su estado. |
| OrdenTrabajoDAO | `int insertar(OrdenTrabajo)`, `OrdenTrabajo buscarPorId(int)`, `void actualizarEstado(int, EstadoOrden)` | CRUD de órdenes. `insertar` devuelve el id generado. |
| AsignacionDAO | `boolean existeSolapamiento(int idRecurso, TipoRecurso tipo, LocalDate fecha, LocalTime ini, LocalTime fin)`, `void registrarPersonal(int idOrden, int idPersonal)`, `void registrarActivo(int idOrden, int idActivo)` | Validación de solapamiento (R1) y alta de asignaciones. |
| CierreOrdenDAO | `int insertar(CierreOrden)` | Alta del cierre dentro de la transacción del CU008. |

**Reglas DAO:** un `Connection` por DAO (o un pool compartido); `PreparedStatement` siempre; mapear `ResultSet` → POJO; traducir entre enums Java y etiquetas `ENUM` de MySQL; no capturar y silenciar excepciones (propagar o envolver en una excepción de capa).

---

## 9. Capa de vista

### GestionOrdenView `<<boundary>>`
Única clase de borde del MVP. Delega todo en `OrdenController`. Para el MVP puede ser consola o GUI simple; lo importante es respetar las firmas.

| Método | Acción |
|---|---|
| `seleccionarSolicitud(int idSolicitud)` | Dispara `generarOrden` (CU006). |
| `asignarRecurso(int idOrden, int idRecurso, TipoRecurso tipo)` | Dispara `validarYAsignar` (CU007); mostrar éxito o "Solapamiento detectado". |
| `registrarCierre(int idOrden, CierreOrden cierre)` | Dispara `cerrarOrden` (CU008). |
| `mostrarResultado(String mensaje)` | Feedback al usuario. |

---

## 10. Flujos (orden de llamadas)

**CU006 — Generar orden:**
```
View.seleccionarSolicitud → ctrl.generarOrden
  → solicitudDAO.buscarPorId
  → ordenDAO.insertar (estado ASIGNADA)
  → solicitudDAO.actualizarEstado (PROGRAMADA)
```

**CU007 — Asignar recursos:**
```
View.asignarRecurso → ctrl.validarYAsignar
  → ordenDAO.buscarPorId (obtener franja)
  → [chequear recurso operativo: R2]
  → asignacionDAO.existeSolapamiento (R1)
  → si libre: asignacionDAO.registrarPersonal/registrarActivo → true
  → si ocupado/no operativo: false (no toca BD)
```

**CU008 — Cierre (transaccional):**
```
View.registrarCierre → ctrl.cerrarOrden
  → setAutoCommit(false)
  → cierreDAO.insertar
  → ordenDAO.actualizarEstado (FINALIZADA)
  → commit  (rollback ante cualquier fallo)
```

---

## 11. Pruebas

### Foco principal: `AsignacionDAO.existeSolapamiento` / `verificarSolapamiento`
Es la lógica crítica. Plan de pruebas:

| Código | Nivel | Técnica | Objetivo |
|---|---|---|---|
| CPC01 | Componente | Cobertura (caja blanca) | Cubrir ramas de la validación de solapamiento. |
| CPF02 | Componente | Partición de equivalencia + análisis de frontera (caja negra) | Comportamiento ante distintos rangos horarios. |
| CPS03 | Sistema | Funcional | Ciclo completo CU006→CU007→CU008. |

**Casos unitarios obligatorios (CPF02)** — derivados de la tabla de verdad de R1. Implementar como tests parametrizados con la orden existente fija en 08:00–12:00 y verificar el resultado esperado de cada franja nueva listada en la sección 6. Incluir explícitamente los casos de borde (08:00 y 12:00 → no bloquea) porque son los que distinguen la desigualdad estricta de la no estricta.

**Casos adicionales recomendados:**
- Recurso no operativo (R2): personal en LICENCIA / activo EN_REPARACION → rechazo.
- Cierre transaccional (R4): simular fallo en el segundo paso y verificar rollback (la orden no debe quedar FINALIZADA y el cierre no debe persistir).
- Avería (R5): `averia=true` sin `detalleAveria` → rechazo de validación.

---

## 12. Convenciones para el agente

- **Nombres BD:** snake_case (`orden_trabajo`). **Nombres Java:** camelCase / PascalCase.
- **Fechas/horas:** `java.time` (`LocalDate`, `LocalTime`). Nunca `java.util.Date`.
- **Dinero/decimales:** `BigDecimal` (no `double`).
- **SQL:** siempre `PreparedStatement` con parámetros; nunca interpolar strings.
- **Conexión:** parametrizar host/usuario/clave por configuración (no hardcodear; no asumir localhost por la infra Cloud).
- **Transacciones:** explícitas en operaciones multi-paso (CU008).
- **Trazabilidad:** cada decisión de diseño remite a un requisito (RF002 → propiedad; RF007 → tablas de asignación + solapamiento; RNF MVC → capas; RNF integridad → InnoDB). Mantenerla.
- Ante un hueco de especificación: aplicar la convención más cercana y dejar `// TODO:` documentado, sin inventar reglas de negocio nuevas.

---

## 13. Fuera de alcance del MVP (no implementar salvo pedido)
- Implementación completa de RBAC y autenticación.
- Reportes gerenciales / dashboards de retroalimentación.
- Gestión de reprogramación automática.
- Notificaciones (WhatsApp/email).

Estos quedan como evolución posterior; no bloquear su incorporación con decisiones de diseño cerradas.
