package com.ef;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionSingleton {
    private String url = "jdbc:mysql://localhost:3306/logdb?serverTimezone=UTC";
    private String user= "logdb";
    private String pass = "logdb";

    private static ConnectionSingleton ourInstance = new ConnectionSingleton();

    public static ConnectionSingleton getInstance() {
        return ourInstance;
    }

    private ConnectionSingleton() {
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }


}
