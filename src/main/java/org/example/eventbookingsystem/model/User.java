package org.example.eventbookingsystem.model;

/**
 * Represents a user in the Event Booking System.
 * A user can sign up, log in, and book tickets.
 * This class stores their essential identity and preferences.
 */
public class User {
    private int id;                  // Unique database ID for the user
    private String username;        // User's login name
    private String password;        // Encrypted password
    private String preferredName;   // Optional friendly name used for display

    /**
     * Constructor to initialize a new user.
     * This version is used after registration or during login.
     *
     * @param id unique user ID
     * @param username the login username
     * @param preferredName user's display name
     */
    public User(int id, String username, String preferredName) {
        this.id = id;
        this.username = username;
        this.preferredName = preferredName;

        // You might log this when a user object is created for debugging
        System.out.println("User object created for username: " + username);
    }

    // Getters to access user info

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPreferredName() {
        return preferredName;
    }

    // Optional: Add setters if needed, depending on usage pattern.
}
