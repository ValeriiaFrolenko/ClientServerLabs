package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:h2:./warehouse;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void init() {
        String sql = """
                CREATE TABLE IF NOT EXISTS products (
                    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name     VARCHAR(255) NOT NULL,
                    category VARCHAR(255) NOT NULL,
                    price    DOUBLE       NOT NULL,
                    quantity INT          NOT NULL
                )
                """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("failed to initialize database", e);
        }
    }
}