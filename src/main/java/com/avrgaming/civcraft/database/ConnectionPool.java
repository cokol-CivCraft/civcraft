package com.avrgaming.civcraft.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionPool {

    private final String dbcUrl;
    private final String user;
    private final String pass;
    private final Connection connection;

    public static void init() throws ClassNotFoundException {
        /* Load any dependent classes. */

        /* load the database driver */
        Class.forName("com.mysql.jdbc.Driver");
    }


    public ConnectionPool(String dbcUrl, String user, String pass) {
        this.dbcUrl = dbcUrl;
        this.user = user;
        this.pass = pass;
        try {
            this.connection = DriverManager.getConnection(dbcUrl, user, pass);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
