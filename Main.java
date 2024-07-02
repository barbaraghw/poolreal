package ConnectionPoolManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        pool pool = new pool();
        pool.start(); // Inicia el hilo del pool de conexiones

        ExecutorService executor = Executors.newFixedThreadPool(5); // Crea un pool de 5 threads

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                Connection connection = null;
                try {
                    connection = pool.getConnection();
                    if (connection != null) {
                        System.out.println("Connection established successfully.");

                        // Utiliza la conexión
                        executeQuery(connection, "SELECT * FROM usuarios");

                    } else {
                        System.out.println("Failed to establish connection.");
                    }
                } catch (SQLException e) {
                    System.out.println("Error obtaining connection: " + e.getMessage());
                } finally {
                    if (connection != null) {
                        pool.releaseConnection(connection); // Siempre liberar la conexión al finalizar
                    }
                }
            });
        }

        executor.shutdown();
        try {
            Thread.sleep(2000); // Espera para demostrar el funcionamiento del pool
            pool.closePool(); // Cierra el pool al finalizar la aplicación
        } catch (InterruptedException | SQLException e) {
            System.err.println("Error closing the connection pool: " + e.getMessage());
        }
    }

    private static void executeQuery(Connection connection, String query) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                // Procesar cada fila del conjunto de resultados si es necesario
            }
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
    }
}
