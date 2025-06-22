package Main;
import java.sql.*;

/**
 *
 * @author Ardhiansyakh
 */
public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/gym_app?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    /**
     *
     * @return
     * @throws SQLException
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver tidak ditemukan.");
        }
    }
}
