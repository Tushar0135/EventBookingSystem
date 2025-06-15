package org.example.eventbookingsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.eventbookingsystem.utilities.DBUtil;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class EditEventController {

    // UI fields to display/edit event details
    @FXML private TextField nameField, venueField, dayField, priceField, capacityField;
    @FXML private Label statusLabel;
    @FXML private TextField soldTicketsField;
    private int eventId; // Holds the ID of the event being edited

    // Called by the parent controller to populate the popup with current event info
    public void setEventDetails(int id, String name, String venue, String day, double price, int capacity,int soldTickets) {
        this.eventId = id;
        nameField.setText(name);
        venueField.setText(venue);
        dayField.setText(day);
        priceField.setText(String.valueOf(price));
        capacityField.setText(String.valueOf(capacity));
        soldTicketsField.setText(String.valueOf(soldTickets));

        System.out.println("Loaded event into edit window: " + name + " (" + venue + ", " + day + ")");
    }

    // Triggered when the user clicks the 'Update' button
    @FXML
    private void handleUpdate() {
        String name = nameField.getText().trim();
        String venue = venueField.getText().trim();
        String day = dayField.getText().trim();
        String priceStr = priceField.getText().trim();
        String capStr = capacityField.getText().trim();

        // Validate that no field is left empty
        if (name.isEmpty() || venue.isEmpty() || day.isEmpty() || priceStr.isEmpty() || capStr.isEmpty()) {
            statusLabel.setText("All fields are required.");
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All fields are required.");

            System.out.println("Update failed: Some input fields were left blank.");
            return;
        }
        List<String> validDays = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        if (!validDays.contains(day)) {
            System.out.println("Invalid day. Please enter a valid day abbreviation (e.g., Mon, Tue, Wed...).");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int capacity = Integer.parseInt(capStr);
            int sold = Integer.parseInt(soldTicketsField.getText());
            //here we will check that admin cant update the capacity less than the sold tickets
            if (capacity < sold) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Capacity cannot be less than sold tickets (" + sold + ").");

                statusLabel.setText("Total tickets cannot be less than sold tickets (" + sold + ").");
                return;
            }
            if (price <= 0) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Price must be greater than 0.");

                statusLabel.setText("Price must be greater than 0.");
                return;
            }

            String updateSQL = "UPDATE events SET name = ?, venue = ?, day = ?, price = ?, totalTickets = ? WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateSQL)) {

                stmt.setString(1, name);
                stmt.setString(2, venue);
                stmt.setString(3, day);
                stmt.setDouble(4, price);
                stmt.setInt(5, capacity);
                stmt.setInt(6, eventId);
                stmt.executeUpdate();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Event updated successfully.");

                System.out.println("Event updated successfully: " + name + " [" + eventId + "]");
            }

            // Close the popup window after successful update
            ((Stage) nameField.getScene().getWindow()).close();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Price and Capacity must be numeric.");

            statusLabel.setText("Price and Capacity must be numeric.");
            System.out.println("Update failed: Invalid number entered.");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error updating event.");
            showAlert(Alert.AlertType.ERROR, "Error", "Error updating event.");

            System.out.println("Update failed: Database exception occurred.");
        }
    }

    // Triggered when the user clicks the 'Cancel' button
    @FXML
    private void handleCancel() {
        System.out.println("Edit canceled by user.");
        ((Stage) nameField.getScene().getWindow()).close();
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
