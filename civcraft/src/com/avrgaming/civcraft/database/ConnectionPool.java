package com.avrgaming.civcraft.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionPool {

	private final String dbcUrl;
	private final String user;
	private final String pass;
	
	public static void init() throws ClassNotFoundException {
		/* Load any dependent classes. */
		
		/* load the database driver */
		Class.forName("com.mysql.jdbc.Driver");
	}
	
	
	public ConnectionPool(String dbcUrl, String user, String pass) {
		this.dbcUrl = dbcUrl;
		this.user = user;
		this.pass = pass;
	}
	
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(dbcUrl, user, pass);
	}
}
