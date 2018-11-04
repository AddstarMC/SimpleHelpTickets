package me.odium.simplehelptickets.database;

import me.odium.simplehelptickets.SimpleHelpTickets;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public class MySQLConnection extends Database {
	private String hostname = "";
    private String port = "";
    private final Properties properties;
	private String database = "";

    public MySQLConnection(ConfigurationSection section, SimpleHelpTickets plugin) {
        super(plugin);
        hostname = section.getString("hostname");
        port = section.getString("hostport");
        database = section.getString("database");
        properties = new Properties();
        properties.put("user", section.getString("user", "user"));
        properties.put("password", section.getString("pass", "password"));
        ConfigurationSection dbprops = section.getConfigurationSection("properties");
        if (dbprops != null) {
            for (Map.Entry<String, Object> entry : dbprops.getValues(false).entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        open();
    }


	/**
	 * open database connection
	 * 
	 * */
    public void open() {
		String url = "";
		try {
			Class.forName("com.mysql.jdbc.Driver");
            url = "jdbc:mysql://" + this.hostname + ":" + this.port + "/" + this.database;
            this.connection = DriverManager.getConnection(url, properties);
        } catch (SQLException e) {
			System.out.print("Could not connect to MySQL server!");
		} catch (ClassNotFoundException e) {
			System.out.print("JDBC Driver not found!");
		}
    }

	/**
	 * close database connection
	 * */
    public void close() {
		try {
			if (connection != null)
				connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createTable() {
		try {
            if (!checkConnection()) open();
			String queryFields = " (id INTEGER AUTO_INCREMENT PRIMARY KEY, description varchar(128), date timestamp, uuid varchar(36), owner varchar(20), world varchar(30), x double(30,20), y double(30,20), z double(30,20), p double(30,20), f double(30,20), adminreply varchar(128), userreply varchar(128), status varchar(16), admin varchar(30) collate latin1_swedish_ci, expiration timestamp NULL DEFAULT NULL)";

			String queryTickets = "CREATE TABLE IF NOT EXISTS SHT_Tickets" + queryFields;
            this.executeStatement(queryTickets);

			String queryIdeas = "CREATE TABLE IF NOT EXISTS SHT_Ideas" + queryFields;
            this.executeStatement(queryIdeas);

		} catch (Exception e) {
			plugin.log.info("[Tickets] MySQL createTable Error: " + e);
		}
	}

	/**
	 * returns the active connection
	 * 
	 * @return Connection
	 * 
	 * */

	// public Connection getConnection() {
	// return connection;
	// }
	public Connection getConnection() {
		if (!checkConnection()) {
			try {
				this.close();
				this.open();
			} catch (Exception e) {
				plugin.log.info("[Tickets] " + "Error: " + e);
			}
		}
		return connection;
	}

	/**
	 * checks if the connection is still active
	 * 
	 * @return true if still active
	 * */
    private boolean checkConnection() {
		try {
			if (connection != null && connection.isValid(2)) {
				return true;
			}
		} catch (SQLException e) {
			return false;
		}
		return false;

	}

	/**
	 * Query the database
	 * 
	 * @param query
	 *            the database query
     *
	 * @throws SQLException if fails
	 * */
    public void executeStatement(String query) throws SQLException {
		Statement statement = null;
		ResultSet result = null;
		try {
			statement = connection.createStatement();
            statement.executeQuery(query);
        } catch (SQLException e) {
			if (e.getMessage().equals("Can not issue data manipulation statements with executeQuery().")) {
				try {
					statement.executeUpdate(query);
				} catch (SQLException ex) {
                    plugin.log.warning(ex.getMessage());
                    plugin.log.warning("Preceded by" + e.getMessage());
                    ex.printStackTrace();
                }
            } else {
                plugin.log.warning(e.getMessage());
                if (plugin.log.getLevel() == Level.FINE) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
	 * Empties a table
	 * 
	 * @param table
	 *            the table to empty
	 * @return true if data-removal was successful.
	 * 
	 * */
	public boolean clearTable(String table) {
		try {
            Statement statement = this.connection.createStatement();
            int result = statement.executeUpdate("TRUNCATE " + table);
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Insert data into a table
	 * 
	 * @param table
	 *            the table to insert data
	 * @param column
	 *            a String[] of the columns to insert to
	 * @param value
	 *            a String[] of the values to insert into the column (value[0]
	 *            goes in column[0])
	 * 
	 * @return true if insertion was successful.
	 * */
	public boolean insert(String table, String[] column, String[] value) {
		Statement statement = null;
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for (String s : column) {
			sb1.append(s).append(",");
		}
		for (String s : value) {
			sb2.append("'").append(s).append("',");
		}
		String columns = sb1.toString().substring(0, sb1.toString().length() - 1);
		String values = sb2.toString().substring(0, sb2.toString().length() - 1);
		try {
			statement = this.connection.createStatement();
			statement.execute("INSERT INTO " + table + "(" + columns + ") VALUES (" + values + ")");
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Delete a table
	 * 
	 * @param table
	 *            the table to delete
	 * @return true if deletion was successful.
	 * */
	public boolean deleteTable(String table) {
		Statement statement = null;
		try {
			if (table.equals("") || table == null) {
				return true;
			}
			statement = connection.createStatement();
			statement.executeUpdate("DROP TABLE " + table);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}