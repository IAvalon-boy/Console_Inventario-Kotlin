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

abstract class ActivoBase(
    val id: Int,
    val inventario: String,
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
    val listaActivos = mutableListOf<ActivoBase>()
    val listaRequerimientos = mutableListOf<TRequerimiento>()
    val mapaCentrosCosto = mutableMapOf<String, TCc>()

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
        if (listaActivos.any { it.inventario == elemento.inventario }) return false
        if (elemento.cc != null && !mapaCentrosCosto.containsKey(elemento.cc)) {
            throw CentroCostoInvalidoException("El Centro de Costo ${elemento.cc} no está registrado en la tabla t_cc.")
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

    fun procesarNuevoRequerimiento(codigoInventario: String, falla: String, usuario: String): TRequerimiento {
        val activoAsociado = listaActivos.find { it.inventario == codigoInventario }
            ?: throw ActivoNoEncontradoException("No se puede generar requerimiento. El activo $codigoInventario no existe.")

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

    fun generarReporteResumen(): String {
        val totalPcs = listaActivos.count { it.tipo == "PC" }
        val totalImpresoras = listaActivos.count { it.tipo == "IMPRESORA" }
        // Agregamos el conteo de todo lo que sea un equipo dinámico (Monitores, UPS, etc.)
        val totalOtros = listaActivos.count { it.tipo != "PC" && it.tipo != "IMPRESORA" }
        val reqPendientes = listaRequerimientos.count { it.estatus == "PENDIENTE" }
        val reqFinalizados = listaRequerimientos.count { it.estatus == "FINALIZADO" }

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
                        "[${it.tipo}] Cód: ${it.inventario} | Activo: $descActivo | " +
                                "CC: ${it.cc} | Ubicación: ${it.ubicacion} | " +
                                "Estado: ${if (it.estadoEquipo == 1) "Activo" else "Baja"}"
                    )
                }
            }

            "2" -> {
                println("\n--- REGISTRAR NUEVO ACTIVO ---")
                try {
                    println("¿Qué tipo de equipo desea registrar?")
                    println("1. Computadora (PC)")
                    println("2. Impresora")
                    println("3. Otro Hardware (UPS, Monitor, Switch, etc.)")
                    print("Seleccione una opción (1/2/3): ")
                    val tipoOpcion = readlnOrNull()?.trim() ?: "3"

                    var tipoDinamicoStr = "GENÉRICO"
                    if (tipoOpcion == "3") {
                        print("Especifique el tipo de equipo (ej. UPS, Monitor, Teléfono): ")
                        tipoDinamicoStr = readlnOrNull()?.trim()?.uppercase() ?: "GENÉRICO"
                    }

                    print("¿Estado de catalogación? (P) Preexistente / (N) Nueva Adquisición: ")
                    val estadoEquipo = readlnOrNull()?.trim()?.uppercase() ?: "P"

                    print("Código de Inventario único (ej. INV-001): ")
                    val codigoInv = readlnOrNull()?.trim()
                    if (codigoInv.isNullOrEmpty()) {
                        throw IllegalArgumentException("El código de inventario no puede estar vacío.")
                    }

                    var numeroActivo: String? = null
                    if (estadoEquipo == "N") {
                        print("Número de Activo Institucional (Obligatorio): ")
                        numeroActivo = readlnOrNull()?.trim()
                        if (numeroActivo.isNullOrEmpty()) {
                            throw IllegalArgumentException("Las nuevas adquisiciones requieren Número de Activo.")
                        }
                    } else {
                        println("Equipo preexistente. Omitiendo captura de Número de Activo.")
                    }

                    print("Marca: ")
                    val marcaInput = readlnOrNull()?.trim() ?: "Genérica"
                    print("Modelo: ")
                    val modeloInput = readlnOrNull()?.trim() ?: "Estándar"
                    print("Centro de Costo (ej. CC01 o CC02): ")
                    val ccInput = readlnOrNull()?.trim() ?: ""
                    print("Responsable asignado: ")
                    val resp = readlnOrNull()
                    print("Ubicación física: ")
                    val ubi = readlnOrNull()

                    val nuevoId = sistema.listaActivos.size + 1
                    val nuevoObjeto: ActivoBase = when (tipoOpcion) {
                        "1" -> {
                            print("Memoria RAM (ej. 8GB): ")
                            val ramIn = readlnOrNull()?.trim()
                            print("Disco Duro (ej. 256GB SSD): ")
                            val hddIn = readlnOrNull()?.trim()
                            TInventPc(nuevoId, codigoInv, numeroActivo, ccInput, resp, ubi, "2200-0000", marcaInput, modeloInput, ramIn, hddIn, "Windows 11", "DHCP")
                        }
                        "2" -> {
                            print("Nivel de tóner actual (ej. 100%): ")
                            val nivelIn = readlnOrNull()?.trim()
                            TImpresores(nuevoId, codigoInv, numeroActivo, ccInput, resp, ubi, "2200-0000", marcaInput, modeloInput, nivelIn)
                        }
                        else -> {
                            TActivoGeneral(nuevoId, codigoInv, numeroActivo, ccInput, resp, ubi, "2200-0000", tipoDinamicoStr, marcaInput, modeloInput)
                        }
                    }

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
                    val cod = readLine()?.trim() ?: ""
                    print("Describa detalladamente la falla detectada: ")
                    val falla = readLine()?.trim() ?: ""
                    if (falla.isEmpty()) {
                        throw IllegalArgumentException("La descripción del daño no puede guardarse en blanco.")
                    }
                    val req = sistema.procesarNuevoRequerimiento(cod, falla, "admin")
                    println("Transacción exitosa. Requerimiento registrado con ID #${req.requerimiento} en estado PENDIENTE.")
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
                    val reqModificar = sistema.listaRequerimientos.find { it.requerimiento == idReq }
                    if (reqModificar != null) {
                        reqModificar.estatus = "FINALIZADO"
                        println("Estado modificado en vivo. Requerimiento #$idReq cambiado a [FINALIZADO].")
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