package database

import java.sql.DriverManager

class GestorBaseDatos {

    private val url = "jdbc:sqlite:biblioteca.db"

    fun getConexion(): java.sql.Connection? {
        var conn: java.sql.Connection? = null
        try {
            conn = DriverManager.getConnection(url)
        } catch (e: Exception) {
            println("❌ Error de conexión: " + e.message)
        }
        return conn
    }

    // ESTA FUNCION SOLO SE LLAMARÁ UNA VEZ AL PRINCIPIO
    fun iniciarSistema() {
        val sqlUsers = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                dni TEXT UNIQUE,
                nombre TEXT,
                password TEXT,
                tipo TEXT,
                email TEXT,
                sancionado_hasta TEXT DEFAULT NULL
            );
        """
        val sqlLibros = """
            CREATE TABLE IF NOT EXISTS libros (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                isbn TEXT,
                titulo TEXT,
                autor TEXT,
                nacionalidad_autor TEXT,
                editorial TEXT,
                editorial_direccion TEXT,
                editorial_telefono TEXT,
                temas TEXT,
                tipo_publicacion TEXT
            );
        """
        val sqlEjemplares = """
            CREATE TABLE IF NOT EXISTS ejemplares (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                libro_id INTEGER,
                codigo_ejemplar TEXT,
                fecha_adquisicion TEXT,
                estado TEXT DEFAULT 'DISPONIBLE',
                FOREIGN KEY(libro_id) REFERENCES libros(id) ON DELETE CASCADE
            );
        """
        val sqlPrestamos = """
            CREATE TABLE IF NOT EXISTS prestamos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER,
                ejemplar_id INTEGER,
                fecha_prestamo TEXT,
                fecha_devolucion_prevista TEXT,
                fecha_devolucion_real TEXT DEFAULT NULL,
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id),
                FOREIGN KEY(ejemplar_id) REFERENCES ejemplares(id)
            );
        """
        val sqlSanciones = """
            CREATE TABLE IF NOT EXISTS sanciones (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario_id INTEGER,
                motivo TEXT,
                fecha_inicio TEXT,
                fecha_fin TEXT,
                dias_sancion INTEGER,
                estado TEXT DEFAULT 'ACTIVA',
                FOREIGN KEY(usuario_id) REFERENCES usuarios(id)
            );
        """

        try {
            val conn = DriverManager.getConnection(url)
            val stmt = conn.createStatement()
            stmt.execute(sqlUsers)
            stmt.execute(sqlLibros)
            stmt.execute(sqlEjemplares)
            stmt.execute(sqlPrestamos)
            stmt.execute(sqlSanciones)

            // Crear admin y datos de ejemplo si no existen
            stmt.execute("""
                INSERT OR IGNORE INTO usuarios (dni, nombre, password, tipo, email) 
                VALUES ('admin', 'Administrador Principal', '1234', 'CONSERJE', 'admin@ies.es');
            """)

            // Insertar más usuarios de ejemplo
            stmt.execute("""
                INSERT OR IGNORE INTO usuarios (dni, nombre, tipo, email) VALUES
                ('12345678A', 'Jesus Lopez Garcia', 'ESTUDIANTE', 'jesus.lopez@sanclemente.com'),
                ('11111111C', 'Pedro Sanchez Ruiz', 'ESTUDIANTE', 'pedro.sanchez@sanclemente.com'),
                ('22222222D', 'Ana Martinez Lopez', 'ESTUDIANTE', 'ana.martinez@sanclemente.com'),
                ('33333333E', 'Carlos Rodriguez Vega', 'ESTUDIANTE', 'carlos.rodriguez@sanclemente.com');
            """)

            // Insertar libros de ejemplo
            stmt.execute("""
                INSERT OR IGNORE INTO libros (isbn, titulo, autor, editorial) VALUES
                ('978-84-123-4567-0', 'Don Quijote de la Mancha', 'Miguel de Cervantes', 'Alfaguara'),
                ('978-84-456-7890-1', 'Kotlin for Beginners', 'JetBrains Team', 'OReilly'),
                ('978-84-789-0123-2', 'Principios de Diseño de Interfaces', 'IES San Clemente', 'Autoedicion'),
                ('978-84-234-5678-3', 'Clean Code', 'Robert C. Martin', 'Prentice Hall'),
                ('978-84-567-8901-4', 'Cien Años de Soledad', 'Gabriel Garcia Marquez', 'Sudamericana');
            """)

            conn.close()
            println("✅ Sistema de Base de Datos inicializado correctamente.")
        } catch (e: Exception) {
            println("Error iniciando sistema: " + e.message)
        }
    }

    // FUNCIÓN DE EMERGENCIA PARA RELLENAR EJEMPLARES
    fun generarEjemplaresFaltantes() {
        val conn = getConexion()
        if (conn != null) {
            try {
                // 1. Buscamos libros que NO tengan ejemplares
                val sql = """
                    SELECT l.id FROM libros l 
                    WHERE NOT EXISTS (SELECT 1 FROM ejemplares e WHERE e.libro_id = l.id)
                """
                val rs = conn.createStatement().executeQuery(sql)

                val idsParaRellenar = ArrayList<Int>()
                while (rs.next()) {
                    idsParaRellenar.add(rs.getInt("id"))
                }

                // 2. Por cada libro sin copias, creamos 3
                val sqlInsert = "INSERT INTO ejemplares (libro_id, codigo_ejemplar, fecha_adquisicion, estado) VALUES (?, ?, ?, 'DISPONIBLE')"
                val ps = conn.prepareStatement(sqlInsert)

                var cont = 0
                for (idLibro in idsParaRellenar) {
                    // Copia 1
                    ps.setInt(1, idLibro)
                    ps.setString(2, "AUTO-" + idLibro + "-1")
                    ps.setString(3, java.time.LocalDate.now().toString())
                    ps.addBatch()

                    // Copia 2
                    ps.setInt(1, idLibro)
                    ps.setString(2, "AUTO-" + idLibro + "-2")
                    ps.setString(3, java.time.LocalDate.now().toString())
                    ps.addBatch()

                    // Copia 3
                    ps.setInt(1, idLibro)
                    ps.setString(2, "AUTO-" + idLibro + "-3")
                    ps.setString(3, java.time.LocalDate.now().toString())
                    ps.addBatch()

                    cont += 3
                }

                if (cont > 0) {
                    ps.executeBatch()
                    println("✅ Se han generado automáticamente $cont ejemplares nuevos para el inventario.")
                }

                conn.close()
            } catch (e: Exception) {
                println("Error generando ejemplares: " + e.message)
            }
        }
    }
}