package org.example.eventbookingsystem.utilities;

/**
 * This class handles the current session for the logged-in user.
 * It stores the username and helps identify if the user is an admin.
 */
public class Session {

    // Keeps track of the currently logged-in user's username
    private static String loggedInUsername;

    /**
     * Sets the current logged-in user's username.
     * This is usually called right after a successful login.
     * @param username The username of the logged-in user
     */
    public static void setLoggedInUsername(String username) {
        loggedInUsername = username;
        System.out.println("Session started for user: " + username);
    }

    /**
     * Returns the username of the currently logged-in user.
     * @return Username as a String
     */
    public static String getLoggedInUsername() {
        return loggedInUsername;
    }

    /**
     * Checks if the currently logged-in user is the admin.
     * @return true if the user is "admin", otherwise false
     */
    public static boolean isAdmin() {
        boolean isAdminUser = "admin".equalsIgnoreCase(loggedInUsername);
        System.out.println("Checking admin status: " + isAdminUser);
        return isAdminUser;
    }

    /**
     * Clears the current session (logout).
     * Resets the stored username to null.
     */
    public static void clear() {
        System.out.println("Session cleared. Logging out user: " + loggedInUsername);
        loggedInUsername = null;
    }
}
