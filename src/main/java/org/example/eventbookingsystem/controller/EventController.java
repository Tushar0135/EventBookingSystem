package org.example.eventbookingsystem.controller;

import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.eventbookingsystem.model.Event;
import org.example.eventbookingsystem.model.User;
import org.example.eventbookingsystem.utilities.DBUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.eventbookingsystem.utilities.PasswordEncryption;
import org.example.eventbookingsystem.utilities.Session;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class EventController {

    @FXML
    private TableView<Event> eventTable;
    @FXML private Label welcomeLabel;
    @FXML private TableColumn<Event, String> nameColumn;
    @FXML private TableColumn<Event, String> venueColumn;
    @FXML private TableColumn<Event, String> dayColumn;
    @FXML private TableColumn<Event, Double> priceColumn;
    @FXML private TableColumn<Event, Integer> soldTicketsColumn;
    @FXML private TableColumn<Event, Integer> totalTicketsColumn;
    @FXML private TableColumn<Event, Integer> availableTicketsColumn;

    private final ObservableList<Event> eventList = FXCollections.observableArrayList();
    private User currentUser;

    @FXML private Button cartButton;
    @FXML private Button logoutButton;
    @FXML private Button disableEventButton;
    @FXML private Button enableEventButton;

    /**
     * Initializer for the event screen.
     * Sets up columns, loads data, configures role-based button visibility, and enables row click.
     */
    @FXML
    public void initialize() {
        loadEventsFromDatIfNeeded(); // Only inserts if DB is empty
        setupTableColumns();
        loadEventsFromDB();

        if (Session.isAdmin()) {
            cartButton.setVisible(false);
            disableEventButton.setVisible(true);
            enableEventButton.setVisible(true);
            System.out.println("Logged in as Admin: showing enable/disable buttons");
        } else {
            cartButton.setVisible(true);
            disableEventButton.setVisible(false);
            enableEventButton.setVisible(false);
            System.out.println("Logged in as User: showing cart button");
        }

        setupRowClick();
    }

    /**
     * Binds event properties to respective table columns.
     */
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        venueColumn.setCellValueFactory(new PropertyValueFactory<>("venue"));
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        soldTicketsColumn.setCellValueFactory(new PropertyValueFactory<>("soldTickets"));
        totalTicketsColumn.setCellValueFactory(new PropertyValueFactory<>("totalTickets"));
        availableTicketsColumn.setCellValueFactory(new PropertyValueFactory<>("availableTickets"));
        System.out.println("Table columns mapped to event properties.");
    }

    /**
     * Reads from `events.dat` and populates the DB only if no events exist.
     */
    private void loadEventsFromDatIfNeeded() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) FROM events")) {

            ResultSet rs = countStmt.executeQuery();
            if (rs.getInt(1) == 0) {
                System.out.println("Database is empty. Loading events from .dat file...");
                try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/events.dat"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(";");
                        if (parts.length == 6) {
                            String name = parts[0];
                            String venue = parts[1];
                            String day = parts[2];
                            double price = Double.parseDouble(parts[3]);
                            int soldTickets = Integer.parseInt(parts[4]);
                            int totalTickets = Integer.parseInt(parts[5]);

                            try (PreparedStatement insertStmt = conn.prepareStatement(
                                    "INSERT INTO events (name, venue, day, price, soldTickets, totalTickets, enabled) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                                insertStmt.setString(1, name);
                                insertStmt.setString(2, venue);
                                insertStmt.setString(3, day);
                                insertStmt.setDouble(4, price);
                                insertStmt.setInt(5, soldTickets);
                                insertStmt.setInt(6, totalTickets);
                                insertStmt.setBoolean(7, true);
                                insertStmt.executeUpdate();
                            }
                        }
                    }
                }
                System.out.println("Events loaded from file into the database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads all enabled events from the database and displays them in the table.
     */
    public void loadEventsFromDB() {
        eventList.clear();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM events WHERE enabled = 1");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Event event = new Event(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("venue"),
                        rs.getString("day"),
                        rs.getDouble("price"),
                        rs.getInt("soldTickets"),
                        rs.getInt("totalTickets"),
                        rs.getBoolean("enabled")
                );
                eventList.add(event);
            }
            eventTable.setItems(eventList);
            System.out.println("Enabled events loaded from database.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Disables a selected event in the database.
     */
    @FXML
    private void handleDisableEvent() {
        Event selected = eventTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Disabling event: " + selected.getName());
            String query = "UPDATE events SET enabled = 0 WHERE id = ?";
            String removeFromCartQuery = "DELETE FROM cart WHERE event_id = ?";

            try (Connection conn = DBUtil.getConnection()){
                 try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
            }

            // Remove this event from all user carts
            try (PreparedStatement stmt2 = conn.prepareStatement(removeFromCartQuery)) {
                stmt2.setInt(1, selected.getId());
                stmt2.executeUpdate();
            }
                loadEventsFromDB();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Enables a selected event in the database.
     */
    @FXML
    private void handleEnableEvent() {
        Event selected = eventTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Enabling event: " + selected.getName());
            String query = "UPDATE events SET enabled = 1 WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
                loadEventsFromDB();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Opens ticket window on double-clicking a row.
     */
    private void setupRowClick() {
        eventTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !eventTable.getSelectionModel().isEmpty()) {
                Event selectedEvent = eventTable.getSelectionModel().getSelectedItem();
                System.out.println("Opening ticket window for: " + selectedEvent.getName());
                openTicketWindow(selectedEvent);
            }
        });
    }

    /**
     * Loads the ticket purchase popup window.
     */
    private void openTicketWindow(Event selectedEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/TicketWindow.fxml"));
            Parent root = loader.load();
            TicketController ticketController = loader.getController();
            ticketController.setEvent(selectedEvent);
            ticketController.setEventController(this);
            Stage stage = new Stage();
            stage.setTitle("Ticket Information");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens cart view window.
     */
    @FXML
    private void handleViewCart(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/CartView.fxml"));
            Parent root = loader.load();

            CartController cartController = loader.getController();
            cartController.setEventController(this);
            Stage stage = new Stage();
            stage.setTitle("Your Cart");
            stage.setScene(new Scene(root));
            stage.show();
            System.out.println("Cart window opened.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (welcomeLabel != null && user.getPreferredName() != null) {
            welcomeLabel.setText("Welcome, " + user.getPreferredName() + "!");
            System.out.println("Dashboard loaded for: " + user.getPreferredName());
        }
    }

    /**
     * Logs the user out and returns to the login screen.
     */
    @FXML
    private void handleLogout() {
        Session.clear();
        System.out.println("User logged out. Returning to login screen.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/login_signup.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Login");
            stage.setScene(new Scene(loginRoot));
            stage.show();

            Stage currentStage = (Stage) eventTable.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the user's order history screen.
     */
    @FXML
    private void handleViewOrders() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/OrderHistoryView.fxml"));
            VBox orderRoot = loader.load();
            Stage orderStage = new Stage();
            orderStage.setTitle("Order History");
            orderStage.setScene(new Scene(orderRoot));
            orderStage.show();
            System.out.println("Order history window opened.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles password change and logs the user out after success.
     */
    @FXML
    private void handleChangePassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Enter New Password");
        dialog.setContentText("New Password:");

        dialog.showAndWait().ifPresent(newPassword -> {
            if (newPassword.length() < 4) {
                showAlert("Password must be at least 4 characters.");
                System.out.println("Password update failed: too short.");
            } else {
                String encrypted = PasswordEncryption.encryptPassword(newPassword);
                updatePasswordInDB(Session.getLoggedInUsername(), encrypted);
                showAlert("Password changed successfully.");
                Session.clear();
                System.out.println("Password changed. Logging user out for security.");

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/login_signup.fxml"));
                    Parent root = loader.load();
                    Stage loginStage = new Stage();
                    loginStage.setTitle("Login");
                    loginStage.setScene(new Scene(root));
                    loginStage.show();

                    Stage currentStage = (Stage) logoutButton.getScene().getWindow();
                    currentStage.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Failed to return to login screen.");
                }
            }
        });
    }

    /**
     * Updates password in the DB for a given user.
     */
    private void updatePasswordInDB(String username, String encryptedPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, encryptedPassword);
            stmt.setString(2, username);
            stmt.executeUpdate();
            System.out.println("Password updated in DB for user: " + username);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error updating password.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Change Password");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
