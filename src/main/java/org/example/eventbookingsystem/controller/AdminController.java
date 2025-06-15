package org.example.eventbookingsystem.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.eventbookingsystem.model.CartItem;
import org.example.eventbookingsystem.utilities.DBUtil;
import org.example.eventbookingsystem.utilities.Session;
import java.sql.*;
import java.util.*;

public class AdminController {

    // UI elements from FXML
    @FXML private TableView<GroupedEvent> groupedEventTable;
    @FXML private TableColumn<GroupedEvent, String> nameColumn;
    @FXML private TableColumn<GroupedEvent, String> detailsColumn;
    @FXML private TableColumn<GroupedEvent, String> statusColumn;

    @FXML private TextField nameField, venueField, dayField, priceField, capacityField;
    @FXML private Button logoutButton;

    // Used to keep track of event name to ID mapping
    private final Map<String, Integer> eventNameToId = new HashMap<>();

    // Called automatically when this controller is initialized
    @FXML
    public void initialize() {
        System.out.println("Admin dashboard initialized.");

        // Bind table columns to GroupedEvent properties
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        handleRowDoubleClick();  // Set double-click listener
        loadGroupedEvents();     // Load all events
    }

    // Load all events and group them logically for display
    private void loadGroupedEvents() {
        System.out.println("Loading grouped events for admin table...");

        ObservableList<GroupedEvent> groupedEvents = FXCollections.observableArrayList();
        eventNameToId.clear();

        // Step 1: Track disabled events for cart cleanup
        List<String> disabledKeys = new ArrayList<>(); // key: name + venue + day

        String sql = "SELECT id, name, venue, day, enabled FROM events ORDER BY name, venue, day";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            String lastName = "";
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String venue = rs.getString("venue");
                String day = rs.getString("day");
                boolean enabled = rs.getBoolean("enabled");

                String venueDay = venue + " - " + day;
                String status = enabled ? "Enabled" : "Disabled";
                String displayName = name.equals(lastName) ? "" : name;
                lastName = name;

                groupedEvents.add(new GroupedEvent(id, name, displayName, venueDay, status));
                eventNameToId.put(name, id);

                // Collect disabled event composite key
                if (!enabled) {
                    disabledKeys.add(name + "|" + venue + "|" + day);
                }
            }

            groupedEventTable.setItems(groupedEvents);

            // Step 2: Remove disabled events from all carts
            for (String username : CartManager.getInstance().getAllUsers()) {
                List<CartItem> items = new ArrayList<>(CartManager.getInstance().getCartItems(username)); // safe iteration
                for (CartItem item : items) {
                    String key = item.getEvent().getName() + "|" + item.getEvent().getVenue() + "|" + item.getEvent().getDay();
                    if (disabledKeys.contains(key)) {
                        System.out.println(key);
                        CartManager.getInstance().removeItemFromCartByComposite(username, item);
                        System.out.println("Removed disabled event from cart of user: " + username);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Called when admin clicks "Add Event"
    @FXML
    private void handleAddEvent() {
        System.out.println("Attempting to add a new event...");

        String name = nameField.getText().trim();
        String venue = venueField.getText().trim();
        String day = dayField.getText().trim();
        double price;
        int capacity;

        try {
            price = Double.parseDouble(priceField.getText().trim());
            capacity = Integer.parseInt(capacityField.getText().trim());
            if (price <= 0) {
                System.out.println("Price must be greater than 0.");
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Price and capacity must be greater than 0.");

                return;
            }

            if (capacity <= 0) {
                System.out.println("Capacity must be greater than 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter valid numeric values for price and capacity.");
            return;
        }
        List<String> validDays = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        if (!validDays.contains(day)) {
            System.out.println("Invalid day. Please enter a valid day abbreviation (e.g., Mon, Tue, Wed...).");
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Day must be a valid short day (Mon to Sun).");
            return;
        }
        if (name.isEmpty() || venue.isEmpty() || day.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Please fill in all the fields.");
            return;
        }


        String checkSQL = "SELECT COUNT(*) FROM events WHERE name = ? AND venue = ? AND day = ?";
        String insertSQL = "INSERT INTO events (name, venue, day, price, soldTickets, totalTickets, enabled) VALUES (?, ?, ?, ?, 0, ?, 1)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {

            checkStmt.setString(1, name);
            checkStmt.setString(2, venue);
            checkStmt.setString(3, day);

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert("Duplicate", "This event already exists.");
                return;
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                insertStmt.setString(1, name);
                insertStmt.setString(2, venue);
                insertStmt.setString(3, day);
                insertStmt.setDouble(4, price);
                insertStmt.setInt(5, capacity);
                insertStmt.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Event added successfully.");

                System.out.println("Event added: " + name + " at " + venue + " on " + day);
            }

            clearInputs();
            loadGroupedEvents();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add event. Please try again.");

            e.printStackTrace();
        }
    }

    // Called when admin clicks "Delete Event"
    @FXML
    private void handleDeleteEvent() {
        GroupedEvent selected = groupedEventTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int id = selected.getId();
        if (id == -1) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Event");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Bookings for this event will remain. Do you want to proceed?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM events WHERE id = ?")) {
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                    System.out.println("Event deleted with ID: " + id);
                    loadGroupedEvents();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Called when admin enables or disables an event
    @FXML private void handleEnableEvent() { updateSelectedEventStatus(true); }
    @FXML private void handleDisableEvent() { updateSelectedEventStatus(false); }

    // Helper to enable/disable selected event
    private void updateSelectedEventStatus(boolean enable) {
        GroupedEvent selected = groupedEventTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        int id = selected.getId();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE events SET enabled = ? WHERE id = ?")) {
            stmt.setBoolean(1, enable);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            System.out.println("Event " + (enable ? "enabled" : "disabled") + " with ID: " + id);
            System.out.println("Removed event from all user carts in memory.");loadGroupedEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Logs out the admin and returns to login screen
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/login_signup.fxml"));
            Parent loginRoot = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Login");
            stage.setScene(new Scene(loginRoot));
            stage.show();

            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();
            Session.clear();
            System.out.println("Admin logged out.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clears the input fields for adding/updating events
    private void clearInputs() {
        nameField.clear();
        venueField.clear();
        dayField.clear();
        priceField.clear();
        capacityField.clear();
    }

    // Shows a dialog box with a message
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Helper class used to display grouped event data in table
    public static class GroupedEvent {
        private final String actualName;
        private final String name; // display name
        private final String details;
        private final String status;
        private final int id;

        public GroupedEvent(int id, String actualName, String nameToDisplay, String details, String status) {
            this.id = id;
            this.actualName = actualName;
            this.name = nameToDisplay;
            this.details = details;
            this.status = status;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDetails() { return details; }
        public String getStatus() { return status; }
        public String getActualName() { return actualName; }

    }

    // Opens the full order view window
    @FXML
    private void handleViewAllOrders() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/AdminOrderView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("All Orders");
            stage.setScene(new Scene(root));
            stage.show();
            System.out.println("Opened All Orders View");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Enables double-click editing of a row
    @FXML
    private void handleRowDoubleClick() {
        groupedEventTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !groupedEventTable.getSelectionModel().isEmpty()) {
                GroupedEvent selected = groupedEventTable.getSelectionModel().getSelectedItem();
                int id = getEventIdFromSelectedRow(selected);
                System.out.println("Opening edit popup for event ID: " + id);
                openEditPopup(id);
            }
        });
    }

    // Helper to get event ID based on selected row values
    private int getEventIdFromSelectedRow(GroupedEvent selected) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM events WHERE name = ? AND venue = ? AND day = ?")) {
            String[] venueDay = selected.getDetails().split(" - ");
            stmt.setString(1, selected.getActualName());
            stmt.setString(2, venueDay[0]);
            stmt.setString(3, venueDay[1]);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Opens the event edit form with current event data
    private void openEditPopup(int eventId) {
        String name = "", venue = "", day = "";
        double price = 0;
        int totalTickets = 0;
        int soldTickets = 0;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM events WHERE id = ?")) {

            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                name = rs.getString("name");
                venue = rs.getString("venue");
                day = rs.getString("day");
                price = rs.getDouble("price");
                totalTickets = rs.getInt("totalTickets");
                soldTickets=rs.getInt("soldTickets");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/EditEvent.fxml"));
            Parent root = loader.load();
            EditEventController controller = loader.getController();
            controller.setEventDetails(eventId, name, venue, day, price, totalTickets,soldTickets);

            Stage stage = new Stage();
            stage.setTitle("Edit Event");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadGroupedEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
