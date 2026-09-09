import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// =========================================================================
// 1. APLICACIÓN DE POO: INTERFACES, HERENCIA Y CLASES
// =========================================================================

// Interfaz para definir el comportamiento del CRUD (Requerimiento 1 y 5)
interface CrudOperations<T> {
    fun crear(elemento: T): Boolean
    fun listar(): List<T>
    fun actualizar(id: String, elemento: T): Boolean
    fun eliminar(id: String): Boolean
}

// Clase abstracta para aplicar Herencia en los Activos del Inventario
abstract class ActivoBase(
    val id: Int,
    val inventario: String, // Clave única (Código de barras)
    var activo: String?,
    var cc: String?,
    var responsable: String?,
    var ubicacion: String?,
    var telefono: String?,
    val tipo: String,
    var marca: String?,
    var modelo: String?,
    var estadoEquipo: Int = 1
)

// Clases de datos concretas mapeadas desde tu SQL
class TInventPc(
    id: Int,
    inventario: String,
    activo: String?,
    cc: String?,
    responsable: String?,
    ubicacion: String?,
    telefono: String?,
    marca: String?,
    modelo: String?,
    var ram: String?,
    var hdd: String?,
    var os: String?,
    var ip: String?
) : ActivoBase(id, inventario, activo, cc, responsable, ubicacion, telefono, "PC", marca, modelo)

class TImpresores(
    id: Int,
    inventario: String,
    activo: String?,
    cc: String?,
    responsable: String?,
    ubicacion: String?,
    telefono: String?,
    marca: String?,
    modelo: String?,
    var nivel: String?
) : ActivoBase(id, inventario, activo, cc, responsable, ubicacion, telefono, "IMPRESORA", marca, modelo)

data class TCc(val cc: String, val nombre: String)

data class TRequerimiento(
    val requerimiento: Int,
    val inventario: String,
    val cc: String?,
    val responsable: String?,
    val falla: String,
    var estatus: String = "PENDIENTE",
    val insertdate: LocalDate = LocalDate.now()
)

// =========================================================================
// 2. MANEJO DE ERRORES, EXCEPCIONES Y LOG EN ARCHIVO DE TEXTO
// =========================================================================
object LoggerServicio {
    private const val LOG_FILE = "error_log.txt"

    fun registrarError(excepcion: Exception, contexto: String) {
        try {
            val fw = FileWriter(LOG_FILE, true)
            val pw = PrintWriter(fw)
            val fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            pw.println("[$fecha] ERROR EN: $contexto")
            pw.println("Mensaje: ${excepcion.message}")
            excepcion.stackTrace.take(3).forEach { pw.println("\tat $it") }
            pw.println("----------------------------------------------------------------------")
            pw.close()
        } catch (e: Exception) {
            println("❌ Error crítico: No se pudo escribir en el archivo de log de errores.")
        }
    }
}

// Excepciones personalizadas para el negocio
class ActivoNoEncontradoException(msg: String) : Exception(msg)
class CentroCostoInvalidoException(msg: String) : Exception(msg)

// =========================================================================
// 3. MANEJO DE COLECCIONES Y LÓGICA DE NEGOCIO CENTRAL
// =========================================================================
class SistemaInventarioService : CrudOperations<ActivoBase> {

    // Uso de colecciones mutables (Listas y Mapas) para gestionar el estado dinámico
    val listaActivos = mutableListOf<ActivoBase>()
    val listaRequerimientos = mutableListOf<TRequerimiento>()
    val mapaCentrosCosto = mutableMapOf<String, TCc>()

    init {
        // Datos iniciales de prueba (Seed Data)
        mapaCentrosCosto["CC01"] = TCc("CC01", "Informática ISSS")
        mapaCentrosCosto["CC02"] = TCc("CC02", "Mantenimiento General")

        listaActivos.add(
            TInventPc(
                1,
                "PC-001",
                "Laptop Core i7",
                "CC01",
                "Daniel",
                "Oficina 4",
                "2200-1111",
                "HP",
                "ProBook",
                "16GB",
                "512GB",
                "Windows 11",
                "192.168.1.50"
            )
        )
        listaActivos.add(
            TImpresores(
                2,
                "IMP-001",
                "Impresora Láser",
                "CC01",
                "Ana",
                "Pasillo B",
                "2200-1112",
                "Epson",
                "L3250",
                "Óptimo"
            )
        )
    }

    // --- REQUERIMIENTO FUNCIONAL 1: MÓDULO DE GESTIÓN PRINCIPAL (CRUD) ---
    override fun crear(elemento: ActivoBase): Boolean {
        if (listaActivos.any { it.inventario == elemento.inventario }) return false
        if (elemento.cc != null && !mapaCentrosCosto.containsKey(elemento.cc)) {
            throw CentroCostoInvalidoException(
                "El Centro de Costo ${elemento.cc} no está registrado en la tabla t_cc."
            )
        }
        return listaActivos.add(elemento)
    }

    override fun listar(): List<ActivoBase> = listaActivos

    override fun actualizar(id: String, elemento: ActivoBase): Boolean {
        val indice = listaActivos.indexOfFirst { it.inventario == id }
        if (indice == -1) {
            throw ActivoNoEncontradoException("El activo con código $id no existe en el sistema.")
        }

        listaActivos[indice] = elemento
        return true
    }

    override fun eliminar(id: String): Boolean {
        val eliminado = listaActivos.removeIf { it.inventario == id }
        if (!eliminado) {
            throw ActivoNoEncontradoException("No se pudo eliminar. El activo $id no existe.")
        }
        return eliminado
    }

    // --- REQUERIMIENTO FUNCIONAL 2: MÓDULO DE PROCESAMIENTO / CÁLCULO ---
    fun procesarNuevoRequerimiento(
        codigoInventario: String,
        falla: String,
        usuario: String
    ): TRequerimiento {
        val activoAsociado = listaActivos.find { it.inventario == codigoInventario }
            ?: throw ActivoNoEncontradoException(
                "No se puede generar requerimiento. El activo $codigoInventario no existe."
            )

        val nuevoId = (listaRequerimientos.maxOfOrNull { it.requerimiento } ?: 0) + 1
        val nuevoReq = TRequerimiento(
            requerimiento = nuevoId,
            inventario = codigoInventario,
            cc = activoAsociado.cc,
            responsable = activoAsociado.responsable,
            falla = falla
        )
        listaRequerimientos.add(nuevoReq)
        return nuevoReq
    }

    // --- REQUERIMIENTO FUNCIONAL 4: GENERACIÓN DE REPORTE O RESUMEN ---
    fun generarReporteResumen(): String {
        val totalPcs = listaActivos.count { it.tipo == "PC" }
        val totalImpresoras = listaActivos.count { it.tipo == "IMPRESORA" }
        val reqPendientes = listaRequerimientos.count { it.estatus == "PENDIENTE" }
        val reqFinalizados = listaRequerimientos.count { it.estatus == "FINALIZADO" }

        return """
        +-------------------------------------------------------+
        |             REPORTE CONSOLIDADO DEL SISTEMA           |
        +-------------------------------------------------------+
          Total Computadoras Registradas: $totalPcs
          Total Impresoras Registradas:  $totalImpresoras
          Requerimientos en Estado PENDIENTE: $reqPendientes
          Requerimientos FINALIZADOS:        $reqFinalizados
          Eficiencia de Cierre Tecnico: ${
            if (listaRequerimientos.isNotEmpty()) {
                reqFinalizados * 100 / listaRequerimientos.size
            } else {
                0
            }
        }%
        +-------------------------------------------------------+
        """.trimIndent()
    }
}

// =========================================================================
// 4. INTERFAZ DE CONSOLA Y VALIDACIÓN DE ENTRADAS
// =========================================================================
fun main() {
    val sistema = SistemaInventarioService()
    var ejecutar = true

    println("=======================================================")
    println("          SISTEMA INVENTARIO SIRN - INICIO CORE        ")
    println("=======================================================")

    while (ejecutar) {
        // --- REQUERIMIENTO FUNCIONAL 3: VISUALIZACIÓN DE RESULTADOS ---
        println("\n=== MENÚ PRINCIPAL MÓVIL (CONSOLA) ===")
        println("1. Listar Activos (t_inventpc / t_impresores)")
        println("2. Registrar Nuevo Activo (PC)")
        println("3. Levantar Requerimiento Técnico")
        println("4. Actualizar Estado de Requerimiento (Dinámico)")
        println("5. Ver Reporte Gerencial del Sistema")
        println("6. Forzar un Error (Prueba de Log de Texto)")
        println("7. Salir")
        print("Seleccione una opción: ")

        // Validación de entradas robusta para evitar caídas del programa
        val opcion = readLine()?.trim() ?: break

        when (opcion) {
            "1" -> {
                println("\n--- LISTADO DE ACTIVOS EN EL SISTEMA ---")
                sistema.listar().forEach {
                    println(
                        "[${it.tipo}] Cód: ${it.inventario} | Desc: ${it.activo} | " +
                            "CC: ${it.cc} | Ubicación: ${it.ubicacion} | " +
                            "Estado: ${if (it.estadoEquipo == 1) "Activo" else "Baja"}"
                    )
                }
            }

            "2" -> {
                println("\n--- REGISTRAR NUEVA COMPUTADORA ---")
                try {
                    print("Código de Inventario único (ej. PC-005): ")
                    val codigo = readLine()?.trim()
                        ?: throw IllegalArgumentException("El código no puede estar vacío")
                    print("Nombre descriptivo del activo: ")
                    val nombre = readLine()
                    print("Centro de Costo (ej. CC01 o CC02): ")
                    val ccInput = readLine()?.trim() ?: ""
                    print("Responsable asignado: ")
                    val resp = readLine()
                    print("Ubicación física: ")
                    val ubi = readLine()

                    val nuevaPc = TInventPc(
                        id = sistema.listaActivos.size + 1,
                        inventario = codigo,
                        activo = nombre,
                        cc = ccInput,
                        responsable = resp,
                        ubicacion = ubi,
                        telefono = "2200-0000",
                        marca = "Dell",
                        modelo = "Optiplex",
                        ram = "8GB",
                        hdd = "256GB SSD",
                        os = "Windows 11",
                        ip = "192.168.1.100"
                    )

                    if (sistema.crear(nuevaPc)) {
                        println("✅ Activo creado exitosamente e insertado en la colección dinámica.")
                    } else {
                        println("⚠️ El código de inventario ya se encuentra duplicado.")
                    }
                } catch (e: CentroCostoInvalidoException) {
                    println("❌ Error de Validación: ${e.message}")
                    LoggerServicio.registrarError(e, "Registro de Activo - CC Inválido")
                } catch (e: Exception) {
                    println("❌ Error inesperado al capturar los datos.")
                    LoggerServicio.registrarError(e, "Registro de Activo - Flujo General")
                }
            }

            "3" -> {
                println("\n--- LEVANTAR REQUERIMIENTO TÉCNICO ---")
                try {
                    print("Ingrese el código de barras/inventario del hardware: ")
                    val cod = readLine()?.trim() ?: ""
                    print("Describa detalladamente la falla detectada: ")
                    val falla = readLine()?.trim() ?: ""
                    if (falla.isEmpty()) {
                        throw IllegalArgumentException(
                            "La descripción del daño no puede guardarse en blanco."
                        )
                    }
                    val req = sistema.procesarNuevoRequerimiento(cod, falla, "admin")
                    println(
                        "✅ Transacción exitosa. Requerimiento registrado con ID " +
                            "#${req.requerimiento} en estado PENDIENTE."
                    )
                } catch (e: ActivoNoEncontradoException) {
                    println("❌ Error Operativo: ${e.message}")
                    LoggerServicio.registrarError(e, "Procesamiento de Requerimiento")
                } catch (e: Exception) {
                    println("❌ Error: Datos de entrada inválidos.")
                    LoggerServicio.registrarError(e, "Captura de Requerimiento")
                }
            }

            "4" -> {
                // --- REQUERIMIENTO FUNCIONAL 5: ACTUALIZACIÓN DINÁMICA DE DATOS ---
                println("\n--- ACTUALIZACIÓN EN TIEMPO REAL DE REPORTES ---")
                print("Número de Requerimiento a dar de alta / finalizar: ")
                val idReqStr = readLine()?.trim() ?: ""
                val idReq = idReqStr.toIntOrNull()
                if (idReq == null) {
                    println("⚠️ Debe digitar un número entero válido.")
                } else {
                    val reqModificar = sistema.listaRequerimientos.find { it.requerimiento == idReq }
                    if (reqModificar != null) {
                        reqModificar.estatus = "FINALIZADO"
                        println(
                            "✅ Estado modificado en vivo. Requerimiento #$idReq " +
                                "cambiado a [FINALIZADO]."
                        )
                    } else {
                        println("❌ No se encontró ningún requerimiento con ese identificador.")
                    }
                }
            }

            "5" -> println("\n" + sistema.generarReporteResumen())

            "6" -> {
                println("\nSimulando un fallo catastrófico en la base de datos...")
                try {
                    sistema.eliminar("CONEXION_FALSA_ERROR")
                } catch (e: Exception) {
                    println(
                        "🔥 Excepción atrapada correctamente. Escribiendo traza " +
                            "en 'error_log.txt'..."
                    )
                    LoggerServicio.registrarError(e, "Módulo de Pruebas de Resiliencia")
                }
            }

            "7" -> {
                println("\nSaliendo del núcleo móvil de consola. ¡Buen día!")
                ejecutar = false
            }

            else -> println("\n⚠️ Comando inválido.")
        }
    }
}