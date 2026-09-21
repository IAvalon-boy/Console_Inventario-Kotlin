# Sistema de Inventario

Aplicación de consola desarrollada en Kotlin para gestionar activos informáticos y requerimientos de soporte técnico.

Permite registrar computadoras, consultar activos, reportar fallas, finalizar requerimientos y generar un resumen del inventario.

## Tecnologías

| Componente | Tecnología |
| --- | --- |
| Lenguaje | Kotlin 2.0.21 |
| Plataforma | JVM |
| Construcción | Maven |
| Dependencia principal | Kotlin Standard Library |
| Plugin de compilación | kotlin-maven-plugin 2.0.21 |
| Plugin de ejecución | exec-maven-plugin 3.5.0 |
| Punto de entrada | `main()` / clase JVM `MainKt` |
| Almacenamiento | Colecciones en memoria |
| Registro de errores | Archivo `error_log.txt` |


## Requisitos

- JDK 17 o 21.
- Maven instalado.
- Variable `JAVA_HOME` configurada con la ruta del JDK.

## Funcionalidades

| Opción | Descripción |
| --- | --- |
| 1. Listar activos | Muestra tipo, código, descripción, centro de costo, ubicación y estado. |
| 2. Registrar computadora | Captura código de inventario, descripción, centro de costo, responsable y ubicación. |
| 3. Levantar requerimiento | Registra una falla asociada a un activo existente. |
| 4. Finalizar requerimiento | Cambia el estado de un requerimiento a `FINALIZADO`. |
| 5. Consultar reporte | Muestra cantidades de equipos, requerimientos y porcentaje de cierre. |
| 6. Probar errores | Provoca una excepción controlada y la registra en un archivo. |
| 7. Salir | Finaliza la aplicación. |

Al registrar una computadora, los campos técnicos no solicitados reciben valores fijos: Dell Optiplex, 8GB de RAM, 256GB SSD, Windows 11, IP `192.168.1.100` y teléfono `2200-0000`.

## Estructura del proyecto

```text
src/
└── main/
    └── kotlin/
        └── Main.kt
pom.xml
Console_Inventario-Kotlin.iml
```



## Modelo de datos

### Activos

`ActivoBase` contiene:

- `id: Int`: identificador numérico.
- `inventario: String`: código utilizado para buscar y gestionar el activo.
- `tipo: String`: `PC` o `IMPRESORA`.
- `estadoEquipo: Int`: inicialmente `1`.
- Campos `String?`: descripción (`activo`), centro de costo (`cc`), responsable, ubicación, teléfono, marca y modelo.

El listado interpreta `estadoEquipo = 1` como `Activo` y cualquier otro valor como `Baja`.

`TInventPc` agrega RAM, almacenamiento, sistema operativo e IP.

`TImpresores` agrega el campo `nivel`.

### Centros de costo

`TCc` contiene el código `cc` y su `nombre`.

### Requerimientos técnicos

`TRequerimiento` contiene:

- Número de requerimiento.
- Código de inventario asociado.
- Centro de costo y responsable copiados del activo.
- Descripción de la falla.
- Estado, inicialmente `PENDIENTE`.
- Fecha de creación mediante `LocalDate.now()`.


### Datos iniciales

| Código | Descripción |
| --- | --- |
| `CC01` | Informática ISSS |
| `CC02` | Mantenimiento General |
| `PC-001` | Laptop Core i7, HP ProBook, responsable Daniel |
| `IMP-001` | Impresora Láser, Epson L3250, responsable Ana |

Ambos activos pertenecen a `CC01`. La colección de requerimientos comienza vacía.

## Reglas y validaciones

- La creación rechaza códigos de inventario duplicados.
- Si el centro de costo no es nulo, debe existir en el mapa de centros de costo.
- Un requerimiento necesita un activo existente.
- El menú exige una descripción de falla no vacía.
- Para finalizar un requerimiento, el menú valida su número mediante `toIntOrNull()` y comprueba que exista.
- Actualizar o eliminar un activo inexistente genera una excepción personalizada.

El parámetro `id: String` de las operaciones CRUD corresponde al código `inventario`, no al identificador numérico del activo.

## Reporte gerencial

El resumen incluye:

- Total de computadoras.
- Total de impresoras.
- Requerimientos pendientes.
- Requerimientos finalizados.
- Porcentaje de cierre técnico.

```text
Porcentaje de cierre = finalizados × 100 / total de requerimientos
```

## Manejo de errores

Se definen dos excepciones de negocio:

- `ActivoNoEncontradoException`.
- `CentroCostoInvalidoException`.

`LoggerServicio` agrega registros a `error_log.txt` en el directorio de trabajo. Cada registro incluye:

- Fecha y hora.
- Contexto de la operación.
- Mensaje de la excepción.
- Primeros tres elementos de la traza.

## Ejemplo de uso

1. Ejecutar `mvn -q compile exec:java`.
2. Seleccionar `1` para consultar los activos iniciales.
3. Seleccionar `3`, ingresar `PC-001` y describir una falla.
4. Seleccionar `5`: aparecerá un requerimiento pendiente.
5. Seleccionar `4` e ingresar el número `1`.
6. Seleccionar `5`: aparecerá un requerimiento finalizado y un cierre del `100%`.
7. Seleccionar `7` para salir.

## Integrantes del grupo

| Nombre                               | N.º de carnet |
|--------------------------------------|---------------|
| Oscar Daniel Diaz Hernandez          | DH252726      |
| Henry Mauricio Peña Ramírez          | PR251335      |
| Manuel Alberto Mata Duran            | MD130678      |
| krissia Lissette Eguizabal Fernandez | EF251345      |



## CARRERA:
Técnico en ingeniería en computación.
ASIGNATURA:
## Desarrollo de Aplicaciones para Dispositivos Móviles
## DOCENTE:
Alexander Alberto Siguenza
## ACTIVIDAD:
Proyecto de cátedra Etapa 2
