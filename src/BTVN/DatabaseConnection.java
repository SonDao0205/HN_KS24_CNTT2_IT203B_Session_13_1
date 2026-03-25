package BTVN;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/jdbc?createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASS = "123";
    public static Connection getConnection() {
        try{
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        }catch(Exception e){
            System.out.println("Error : " + e.getMessage());
        }
    };
}
