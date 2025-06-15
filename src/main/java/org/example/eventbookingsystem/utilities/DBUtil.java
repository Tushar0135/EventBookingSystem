package org.example.eventbookingsystem.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class for handling database connection and setup for the Event Booking System.
 * This handles connecting to the SQLite database and creating tables if they do not exist.
 */
public class DBUtil {

    // Path to your SQLite database file
    private static final String DB_URL = "jdbc:sqlite:src/event_booking.db";

    /**
     * Provides a reusable connection to the database.
     * Call this whenever you need to perform a SQL operation.
     */
    public static Connection getConnection() throws SQLException {
        System.out.println("Connecting to the database...");
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initializes the database by creating required tables if they are missing.
     * This should be called once at application startup.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            System.out.println("Database connection established. Now creating tables if not present...");

            // SQL for Users table
            String userTable = """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password TEXT NOT NULL,
                        preferredName TEXT NOT NULL
                    );
                    """;

            // SQL for Events table
            String eventsTable = """
                    CREATE TABLE IF NOT EXISTS events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        venue TEXT NOT NULL,
                        day TEXT NOT NULL,
                        price REAL NOT NULL,
                        soldTickets INTEGER NOT NULL,
                        totalTickets INTEGER NOT NULL,
                        enabled BOOLEAN DEFAULT 1
                    );
                    """;

            // SQL for Cart table
            String cartTable = """
                    CREATE TABLE IF NOT EXISTS cart (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          username TEXT NOT NULL,
                          event_name TEXT NOT NULL,
                          event_venue TEXT NOT NULL,
                          event_day TEXT NOT NULL,
                          event_price REAL NOT NULL,
                          quantity INTEGER NOT NULL
                    );
                    """;

            // SQL for Orders table
            String ordersTable = """
                    CREATE TABLE IF NOT EXISTS orders (
                        orderNumber TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        eventName TEXT NOT NULL,
                        venue TEXT NOT NULL,
                        day TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        totalPrice REAL NOT NULL,
                        dateTime TEXT NOT NULL
                    );
                    """;

            // Executing all table creation statements
            stmt.execute(userTable);
            System.out.println("Users table created or already exists.");

            stmt.execute(eventsTable);
            System.out.println("Events table created or already exists.");

            stmt.execute(cartTable);
            System.out.println("Cart table created or already exists.");

            stmt.execute(ordersTable);
            System.out.println("Orders table created or already exists.");

            System.out.println("All required tables have been initialized.");

        } catch (SQLException e) {
            System.out.println("Error occurred during database initialization.");
            e.printStackTrace();
        }
    }
}
