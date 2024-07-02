package ConnectionPoolManager;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class pool extends Thread {
    private static final String URL = "jdbc:postgresql://localhost:5432/productos";
    private static final String USER = "postgres";
    private static final String PASS = "1234";
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_POOL_SIZE = 5;
    private static final int MAX_CONNECTION_NUMBER = 30;
    private int connectionCounter = 1;

    private final BlockingQueue<Connection> pool;

    public pool() {
        this.pool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);

        // Initialize the pool with minimum connections
        for (int i = 0; i < MIN_POOL_SIZE; i++) {
            try {
                Connection connection = DriverManager.getConnection(URL, USER, PASS);
                pool.offer(connection);
            } catch (SQLException e) {
                System.err.println("Error initializing the connection pool: " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                try {
                    while (pool.size() >= MAX_POOL_SIZE) {
                        wait();
                    }
                    Connection connection = createConnection();
                    if (connection != null) {
                        pool.offer(connection);
                    }
                    notifyAll();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Connection createConnection() {
        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASS);
            int currentConnectionNumber = connectionCounter;
            connectionCounter += 2;
            if (connectionCounter > MAX_CONNECTION_NUMBER) {
                connectionCounter = 2; // Reiniciar el contador si supera el máximo
            }
            System.out.println("Se ha creado la conexión número: " + currentConnectionNumber);
            return connection;
        } catch (SQLException e) {
            // Manejar la excepción de conexión aquí sin mostrar el mensaje de error
            return null; // Devolver null para indicar que no se pudo crear la conexión
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        while (pool.isEmpty()) {
            if (pool.size() < MAX_POOL_SIZE) {
                Connection connection = createConnection();
                if (connection != null) {
                    pool.offer(connection);
                }
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        notifyAll();
        return pool.poll();
    }

    public synchronized void releaseConnection(Connection connection) {
        try {
            connection.close(); // Cerrar la conexión
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        } finally {
            pool.offer(connection); // Devolver la conexión al pool
            notifyAll();
        }
    }

    public synchronized void closePool() throws SQLException {
        for (Connection connection : pool) {
            connection.close();
        }
        pool.clear();
    }

    public static void main(String[] args) {
        pool connectionPool = new pool();
        connectionPool.start(); // Inicia el hilo del pool de conexiones

        // Ejemplo de uso continuo del pool (puedes ajustar según tu necesidad)
        while (true) {
            try {
                Connection connection = connectionPool.getConnection();
                // Utilizar la conexión según sea necesario
                // Por ejemplo, realizar consultas o actualizaciones en la base de datos

                // Simular un uso de conexión
                Thread.sleep(2000); // Esperar 2 segundos

                // Liberar la conexión al pool
                connectionPool.releaseConnection(connection);
            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
