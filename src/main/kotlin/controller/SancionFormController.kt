package controller

import database.GestorBaseDatos
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.stage.Stage
import java.time.LocalDate

class SancionFormController {

    @FXML private lateinit var txtBuscar: TextField
    @FXML private lateinit var cmbAlumnos: ComboBox<ComboAlumno>
    @FXML private lateinit var txtMotivo: TextField
    @FXML private lateinit var spinnerDias: Spinner<Int>
    @FXML private lateinit var btnCancelar: Button
    @FXML private lateinit var btnGuardar: Button

    class ComboAlumno(val id: Int, val nombre: String) {
        override fun toString(): String = nombre
    }

    @FXML
    fun initialize() {
        // Configurar spinner
        spinnerDias.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 1)
        spinnerDias.isEditable = true

        cargarAlumnos(cmbAlumnos, "")

        txtBuscar.textProperty().addListener { _, _, nuevoTexto ->
            cargarAlumnos(cmbAlumnos, nuevoTexto)
            cmbAlumnos.show()
        }
    }

    @FXML
    fun handleCancelar() {
        navegarASanciones()
    }

    @FXML
    fun handleGuardar() {
        val alumno = cmbAlumnos.value
        val motivo = txtMotivo.text
        val dias = spinnerDias.value

        if (alumno == null || motivo.isBlank()) {
            val alerta = Alert(Alert.AlertType.WARNING)
            alerta.contentText = "Debes elegir un alumno y escribir un motivo."
            alerta.showAndWait()
        } else {
            guardarSancionManual(alumno.id, motivo, dias)
            navegarASanciones()
        }
    }

    private fun cargarAlumnos(cmb: ComboBox<ComboAlumno>, filtro: String) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()

        val seleccionPrevia = cmb.value
        cmb.items.clear()

        if (conn != null) {
            try {
                val sql = "SELECT id, nombre FROM usuarios WHERE tipo = 'ESTUDIANTE' AND nombre LIKE ?"
                val ps = conn.prepareStatement(sql)
                ps.setString(1, "%$filtro%")

                val rs = ps.executeQuery()
                while (rs.next()) {
                    cmb.items.add(ComboAlumno(rs.getInt("id"), rs.getString("nombre")))
                }
                conn.close()

                if (filtro.isEmpty() && seleccionPrevia != null) {
                    cmb.value = seleccionPrevia
                } else if (cmb.items.isNotEmpty()) {
                    cmb.selectionModel.selectFirst()
                }

            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun guardarSancionManual(idUsuario: Int, motivo: String, dias: Int) {
        val gestor = GestorBaseDatos()
        val conn = gestor.getConexion()
        if (conn != null) {
            try {
                val fechaHoy = LocalDate.now()
                val fechaFin = fechaHoy.plusDays(dias.toLong())

                val sql = """
                    INSERT INTO sanciones (usuario_id, motivo, fecha_inicio, fecha_fin, dias_sancion, estado)
                    VALUES (?, ?, ?, ?, ?, 'ACTIVA')
                """
                val ps = conn.prepareStatement(sql)
                ps.setInt(1, idUsuario)
                ps.setString(2, motivo)
                ps.setString(3, fechaHoy.toString())
                ps.setString(4, fechaFin.toString())
                ps.setInt(5, dias)
                ps.executeUpdate()

                conn.close()
            } catch (e: Exception) {
                println(e.message)
            }
        }
    }

    private fun navegarASanciones() {
        val stage = btnCancelar.scene.window as Stage
        val loader = FXMLLoader(javaClass.getResource("/fxml/sanciones.fxml"))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 1150.0, 750.0)
        stage.isMaximized = true
        stage.title = "Registro de Sanciones"
    }
}