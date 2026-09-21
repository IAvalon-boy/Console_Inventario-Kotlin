import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// =========================================================================
// 1. APLICACIÓN DE POO: INTERFACES, HERENCIA Y CLASES
// =========================================================================

interface CrudOperations<T> {
    fun crear(elemento: T): Boolean
    fun listar(): List<T>
    fun actualizar(id: String, elemento: T): Boolean
    fun eliminar(id: String): Boolean
}

enum class EstadoEquipo {
    ACTIVO,
    BAJA
}

enum class EstadoRequerimiento {
    PENDIENTE,
    FINALIZADO
}

enum class TipoActivo {
    PC,
    IMPRESORA
}

// Clase abstracta para aplicar Herencia en los Activos del Inventario
abstract class ActivoBase(
    val id: Int,
    val inventario: String,
    var activo: String?,
    var cc: String?,
    var responsable: String?,
    var ubicacion: String?,
    var telefono: String?,
    val tipo: TipoActivo,
    var marca: String?,
    var modelo: String?,
    var estadoEquipo: EstadoEquipo = EstadoEquipo.ACTIVO
)

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
) : ActivoBase(id, inventario, activo, cc, responsable, ubicacion, telefono, TipoActivo.PC, marca, modelo)

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
) : ActivoBase(id, inventario, activo, cc, responsable, ubicacion, telefono, TipoActivo.IMPRESORA, marca, modelo)

class TActivoGeneral(
    id: Int,
    inventario: String,
    activo: String?,
    cc: String?,
    responsable: String?,
    ubicacion: String?,
    telefono: String?,
    tipoDinamico: String,
    marca: String?,
    modelo: String?
) : ActivoBase(id, inventario, activo, cc, responsable, ubicacion, telefono, tipoDinamico, marca, modelo)

data class TCc(val cc: String, val nombre: String)

data class TRequerimiento(
    val requerimiento: Int,
    val inventario: String,
    val cc: String?,
    val responsable: String?,
    val falla: String,
    val usuarioRegistro: String,
    var estatus: EstadoRequerimiento = EstadoRequerimiento.PENDIENTE,
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
            println("Error crítico: No se pudo escribir en el archivo de log de errores.")
        }
    }
}

class ActivoNoEncontradoException(msg: String) : Exception(msg)
class CentroCostoInvalidoException(msg: String) : Exception(msg)

// =========================================================================
// 3. MANEJO DE COLECCIONES Y LÓGICA DE NEGOCIO CENTRAL
// =========================================================================
class SistemaInventarioService : CrudOperations<ActivoBase> {

    // Uso de colecciones mutables (Listas y Mapas) para gestionar el estado dinámico
    private val listaActivos = mutableListOf<ActivoBase>()
    private val listaRequerimientos = mutableListOf<TRequerimiento>()
    private val mapaCentrosCosto = mutableMapOf<String, TCc>()

    init {
        mapaCentrosCosto["CC01"] = TCc("CC01", "Informática ISSS")
        mapaCentrosCosto["CC02"] = TCc("CC02", "Mantenimiento General")

        listaActivos.add(
            TInventPc(
                1, "PC-001", "Laptop Core i7", "CC01", "Daniel", "Oficina 4",
                "2200-1111", "HP", "ProBook", "16GB", "512GB", "Windows 11", "192.168.1.50"
            )
        )
        listaActivos.add(
            TImpresores(
                2, "IMP-001", "Impresora Láser", "CC01", "Ana", "Pasillo B",
                "2200-1112", "Epson", "L3250", "Óptimo"
            )
        )
    }

    override fun crear(elemento: ActivoBase): Boolean {
        validarActivo(elemento)
        if (listaActivos.any { it.inventario.equals(elemento.inventario, ignoreCase = true) }) return false
        return listaActivos.add(elemento)
    }

    override fun listar(): List<ActivoBase> = listaActivos.toList()

    override fun actualizar(id: String, elemento: ActivoBase): Boolean {
        val indice = listaActivos.indexOfFirst { it.inventario == id }
        if (indice == -1) {
            throw ActivoNoEncontradoException("El activo con código $id no existe en el sistema.")
        }

        validarActivo(elemento)
        if (listaActivos.any { it.inventario == elemento.inventario && it.inventario != id }) {
            throw IllegalArgumentException("El código de inventario ya pertenece a otro activo.")
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
        val codigoNormalizado = codigoInventario.trim()
        val fallaNormalizada = falla.trim()
        val usuarioNormalizado = usuario.trim()
        require(codigoNormalizado.isNotBlank()) { "El código de inventario es obligatorio." }
        require(fallaNormalizada.isNotBlank()) { "La descripción de la falla es obligatoria." }
        require(usuarioNormalizado.isNotBlank()) { "El usuario que registra el requerimiento es obligatorio." }

        val activoAsociado = listaActivos.find { it.inventario == codigoNormalizado }
            ?: throw ActivoNoEncontradoException(
                "No se puede generar requerimiento. El activo $codigoNormalizado no existe."
            )

        val nuevoId = (listaRequerimientos.maxOfOrNull { it.requerimiento } ?: 0) + 1
        val nuevoReq = TRequerimiento(
            requerimiento = nuevoId,
            inventario = codigoNormalizado,
            cc = activoAsociado.cc,
            responsable = activoAsociado.responsable,
            falla = fallaNormalizada,
            usuarioRegistro = usuarioNormalizado
        )
        listaRequerimientos.add(nuevoReq)
        return nuevoReq
    }

    fun siguienteIdActivo(): Int = (listaActivos.maxOfOrNull { it.id } ?: 0) + 1

    fun finalizarRequerimiento(id: Int): Boolean {
        val requerimiento = listaRequerimientos.find { it.requerimiento == id } ?: return false
        requerimiento.estatus = EstadoRequerimiento.FINALIZADO
        return true
    }

    private fun validarActivo(elemento: ActivoBase) {
        require(elemento.inventario.isNotBlank()) { "El código de inventario es obligatorio." }
        require(!elemento.activo.isNullOrBlank()) { "La descripción del activo es obligatoria." }
        require(!elemento.ubicacion.isNullOrBlank()) { "La ubicación del activo es obligatoria." }
        require(!elemento.marca.isNullOrBlank()) { "La marca del activo es obligatoria." }
        require(!elemento.modelo.isNullOrBlank()) { "El modelo del activo es obligatorio." }

        val centroCosto = elemento.cc?.trim()
        if (!centroCosto.isNullOrEmpty() && !mapaCentrosCosto.containsKey(centroCosto)) {
            throw CentroCostoInvalidoException(
                "El Centro de Costo $centroCosto no está registrado en la tabla t_cc."
            )
        }
    }

    // --- REQUERIMIENTO FUNCIONAL 4: GENERACIÓN DE REPORTE O RESUMEN ---
    fun generarReporteResumen(): String {
        val totalPcs = listaActivos.count { it.tipo == TipoActivo.PC }
        val totalImpresoras = listaActivos.count { it.tipo == TipoActivo.IMPRESORA }
        val reqPendientes = listaRequerimientos.count { it.estatus == EstadoRequerimiento.PENDIENTE }
        val reqFinalizados = listaRequerimientos.count { it.estatus == EstadoRequerimiento.FINALIZADO }

        return """
        +-------------------------------------------------------+
        |             REPORTE CONSOLIDADO DEL SISTEMA           |
        +-------------------------------------------------------+
          Total Computadoras Registradas: $totalPcs
          Total Impresoras Registradas:   $totalImpresoras
          Total Otros Equipos (Dinámico): $totalOtros
          Requerimientos en Estado PENDIENTE: $reqPendientes
          Requerimientos FINALIZADOS:         $reqFinalizados
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
        println("\n=== MENÚ PRINCIPAL MÓVIL (CONSOLA) ===")
        println("1. Listar Activos (t_inventpc / t_impresores)")
        println("2. Registrar Nuevo Activo (Dinámico)")
        println("3. Levantar Requerimiento Técnico")
        println("4. Actualizar Estado de Requerimiento (Dinámico)")
        println("5. Ver Reporte Gerencial del Sistema")
        println("6. Forzar un Error (Prueba de Log de Texto)")
        println("7. Salir")
        print("Seleccione una opción: ")

        val opcion = readLine()?.trim() ?: break

        when (opcion) {
            "1" -> {
                println("\n--- LISTADO DE ACTIVOS EN EL SISTEMA ---")
                sistema.listar().forEach {
                    val descActivo = it.activo ?: "N/A (Preexistente)"
                    println(
                        "[${it.tipo}] Cód: ${it.inventario} | Desc: ${it.activo} | " +
                            "CC: ${it.cc} | Ubicación: ${it.ubicacion} | " +
                            "Estado: ${it.estadoEquipo}"
                    )
                }
            }

            "2" -> {
                println("\n--- REGISTRAR NUEVO ACTIVO ---")
                try {
                    print("Código de Inventario único (ej. PC-005): ")
                    val codigo = readLine()?.trim().orEmpty()
                    require(codigo.isNotBlank()) { "El código no puede estar vacío" }
                    print("Nombre descriptivo del activo: ")
                    val nombre = readLine()?.trim()
                    print("Centro de Costo (ej. CC01 o CC02): ")
                    val ccInput = readLine()?.trim()?.ifBlank { null }
                    print("Responsable asignado: ")
                    val resp = readLine()?.trim()
                    print("Ubicación física: ")
                    val ubi = readLine()?.trim()

                    val nuevaPc = TInventPc(
                        id = sistema.siguienteIdActivo(),
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

                    if (sistema.crear(nuevoObjeto)) {
                        println("Activo registrado exitosamente en el sistema.")
                    } else {
                        println("El código de inventario ya se encuentra duplicado.")
                    }
                } catch (e: CentroCostoInvalidoException) {
                    println("Error de Validación: ${e.message}")
                    LoggerServicio.registrarError(e, "Registro de Activo - CC Inválido")
                } catch (e: IllegalArgumentException) {
                    println("Entrada Inválida: ${e.message}")
                    LoggerServicio.registrarError(e, "Registro de Activo - Datos incompletos")
                } catch (e: Exception) {
                    println("Error inesperado al capturar los datos.")
                    LoggerServicio.registrarError(e, "Registro de Activo - Flujo General")
                }
            }

            "3" -> {
                println("\n--- LEVANTAR REQUERIMIENTO TÉCNICO ---")
                try {
                    print("Ingrese el código de barras/inventario del hardware: ")
                    val cod = readLine()?.trim().orEmpty()
                    print("Describa detalladamente la falla detectada: ")
                    val falla = readLine()?.trim().orEmpty()
                    print("Usuario que registra el requerimiento: ")
                    val usuario = readLine()?.trim().orEmpty()
                    val req = sistema.procesarNuevoRequerimiento(cod, falla, usuario)
                    println(
                        "✅ Transacción exitosa. Requerimiento registrado con ID " +
                            "#${req.requerimiento} en estado PENDIENTE."
                    )
                } catch (e: ActivoNoEncontradoException) {
                    println("Error Operativo: ${e.message}")
                    LoggerServicio.registrarError(e, "Procesamiento de Requerimiento")
                } catch (e: Exception) {
                    println("Error: Datos de entrada inválidos.")
                    LoggerServicio.registrarError(e, "Captura de Requerimiento")
                }
            }

            "4" -> {
                println("\n--- ACTUALIZACIÓN EN TIEMPO REAL DE REPORTES ---")
                print("Número de Requerimiento a dar de alta / finalizar: ")
                val idReqStr = readLine()?.trim() ?: ""
                val idReq = idReqStr.toIntOrNull()
                if (idReq == null) {
                    println("Debe digitar un número entero válido.")
                } else {
                    if (sistema.finalizarRequerimiento(idReq)) {
                        println(
                                "✅ Estado modificado en vivo. Requerimiento #$idReq " +
                                "cambiado a [FINALIZADO]."
                        )
                    } else {
                        println("No se encontró ningún requerimiento con ese identificador.")
                    }
                }
            }

            "5" -> println("\n" + sistema.generarReporteResumen())

            "6" -> {
                println("\nSimulando un fallo catastrófico en la base de datos...")
                try {
                    sistema.eliminar("CONEXION_FALSA_ERROR")
                } catch (e: Exception) {
                    println("Excepción atrapada correctamente. Escribiendo traza en 'error_log.txt'...")
                    LoggerServicio.registrarError(e, "Módulo de Pruebas de Resiliencia")
                }
            }

            "7" -> {
                println("\nSaliendo del núcleo móvil de consola. ¡Buen día!")
                ejecutar = false
            }

            else -> println("\n Comando inválido.")
        }
    }
}