package org.example.eventbookingsystem.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.eventbookingsystem.model.User;
import org.example.eventbookingsystem.utilities.DBUtil;
import org.example.eventbookingsystem.utilities.PasswordEncryption;
import org.example.eventbookingsystem.utilities.Session;
import java.io.IOException;
import java.sql.*;

public class LoginSignUpController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField preferredNameField;
    @FXML private Label statusLabel;
    @FXML private RadioButton loginRadio;
    @FXML private RadioButton signupRadio;
    @FXML private final ToggleGroup loginSignUpToggleGroup = new ToggleGroup();

    /**
     * Sets up the toggle group and manages enabling/disabling the Preferred Name field.
     */
    @FXML
    public void initialize() {
        System.out.println("Checking and initializing database if needed...");
        DBUtil.initializeDatabase();
        loginRadio.setToggleGroup(loginSignUpToggleGroup);
        signupRadio.setToggleGroup(loginSignUpToggleGroup);
        preferredNameField.setDisable(true);

        loginSignUpToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            preferredNameField.setDisable(loginRadio.isSelected());
            System.out.println("Toggle switched: " + (loginRadio.isSelected() ? "Login mode" : "Sign Up mode"));
        });
    }

    /**
     * Handles login or signup based on selected toggle option.
     * Validates inputs, communicates with DB, and navigates to dashboard.
     */
    @FXML
    public void handleContinue(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String preferredName = preferredNameField.getText();

        if (username.isEmpty() || password.isEmpty() || (signupRadio.isSelected() && preferredName.isEmpty())) {
            statusLabel.setText("All fields are required.");
            System.out.println("Validation failed: Some fields are missing.");
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {

            if (loginRadio.isSelected()) {
                // Check for admin login
                if (username.equals("admin") && password.equals("Admin321")) {
                    Session.setLoggedInUsername("admin");
                    System.out.println("Admin logged in.");
                    loadAdminDashboard();
                    return;
                }

                String encryptedPassword = PasswordEncryption.encryptPassword(password);
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
                stmt.setString(1, username);
                stmt.setString(2, encryptedPassword);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("Login successful for user: " + username);
                    loadEventsPage(new User(rs.getInt("id"), rs.getString("username"), rs.getString("preferredName")));
                } else {
                    statusLabel.setText("Invalid login.");
                    System.out.println("Login failed: Invalid credentials.");
                }

            } else {
                // Signup process
                PreparedStatement check = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
                check.setString(1, username);
                ResultSet rs = check.executeQuery();

                if (rs.next()) {
                    statusLabel.setText("Username already exists.");
                    System.out.println("Signup failed: Username already exists.");
                } else {
                    String encryptedPassword = PasswordEncryption.encryptPassword(password);
                    PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO users (username, password, preferredName) VALUES (?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    insert.setString(1, username);
                    insert.setString(2, encryptedPassword);
                    insert.setString(3, preferredName);
                    insert.executeUpdate();

                    ResultSet generatedKeys = insert.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);
                        System.out.println("Signup successful for user: " + username);
                        loadEventsPage(new User(userId, username, preferredName));
                    }
                }
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error: " + e.getMessage());
            System.out.println("Exception occurred: " + e.getMessage());
        }
    }

    /**
     * Loads the user event page after successful login or signup.
     */
    private void loadEventsPage(User user) throws IOException {
        Session.setLoggedInUsername(user.getUsername());
        System.out.println("Loading event page for: " + user.getUsername());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/events.fxml"));
        BorderPane root = loader.load();
        EventController controller = loader.getController();
        controller.setCurrentUser(user);

        Stage stage = new Stage();
        stage.setTitle("Browse Events");
        stage.setScene(new Scene(root));
        stage.show();

        ((Stage) usernameField.getScene().getWindow()).close();
    }

    /**
     * Loads the admin dashboard for admin user.
     */
    private void loadAdminDashboard() throws IOException {
        System.out.println("Redirecting to Admin Dashboard...");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/AdminDashboard.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setTitle("Admin Dashboard");
        stage.setScene(new Scene(root));
    }
}
