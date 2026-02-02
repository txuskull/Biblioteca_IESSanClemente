package view

import database.GestorBaseDatos
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class Main : Application() {
    override fun start(stage: Stage) {
        // INICIALIZAR BASE DE DATOS
        val gestor = GestorBaseDatos()

        // 1. crea las tablas y mete los usuarios y libros base
        gestor.iniciarSistema()

        // 2. genera las copias fisicas (ejemplares) de los libros nuevos
        gestor.generarEjemplaresFaltantes()

        // CARGAR LOGIN
        val loader = FXMLLoader(javaClass.getResource("/fxml/login.fxml"))
        val root = loader.load<Any>()

        stage.scene = Scene(root as javafx.scene.Parent, 900.0, 600.0)
        stage.title = "Biblioteca IES San Clemente - Acceso Seguro"
        stage.show()
    }
}

fun main() {
    Application.launch(Main::class.java)
}