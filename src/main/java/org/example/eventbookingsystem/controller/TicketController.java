package org.example.eventbookingsystem.controller;

import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.eventbookingsystem.model.Event;
import org.example.eventbookingsystem.model.CartItem;
import org.example.eventbookingsystem.utilities.DBUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.eventbookingsystem.utilities.Session;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TicketController {

    @FXML private Label eventLabel;
    @FXML private Label availableTicketsLabel;
    @FXML private TextField quantityField;
    @FXML private Button addToCartButton;
    @FXML private Button confirmButton;
    @FXML private Label statusLabel;
    @FXML private VBox root;
    @FXML private TableView<CartItem> cartTable;

    private Event selectedEvent;
    private CartManager cartManager = CartManager.getInstance();
    private EventController eventController;

    /**
     * Called externally to set the event details before this screen shows.
     */
    public void setEvent(Event event) {
        this.selectedEvent = event;
        eventLabel.setText("Event: " + event.getName());
        availableTicketsLabel.setText("Available Tickets: " + event.getAvailableTickets());
        System.out.println("Opened ticket window for event: " + event.getName());
    }

    /**
     * Allows the EventController to be notified after updates (like ticket purchase).
     */
    public void setEventController(EventController eventController) {
        this.eventController = eventController;
    }

    /**
     * Handles the logic when a user clicks "Add to Cart".
     * This includes validation, database update, and UI refresh.
     */
    @FXML
    public void handleAddToCart() {
        try {
            int quantity = Integer.parseInt(quantityField.getText());

            if (quantity <= 0) {
                statusLabel.setText("Quantity must be positive.");
                System.out.println("Invalid quantity entered: " + quantity);
                return;
            }

            if (quantity > selectedEvent.getAvailableTickets()) {
                statusLabel.setText("Not enough tickets available.");
                System.out.println("Requested quantity exceeds available tickets.");
                return;
            }

            if (!isBookingAllowedToday(selectedEvent.getDay())) {
                statusLabel.setText("Cannot book events earlier this week.");
                System.out.println("Booking not allowed today for event day: " + selectedEvent.getDay());
                return;
            }

            // Add to in-memory cart
            String currentUser = Session.getLoggedInUsername();
            CartItem cartItem = new CartItem(selectedEvent, quantity, selectedEvent.getPrice());
            cartManager.addToCart(currentUser, cartItem);
            System.out.println("Added to cart: " + quantity + " tickets for " + selectedEvent.getName());

            // Update ticket count in the database
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE events SET soldTickets = soldTickets + ? WHERE id = ?")) {
                stmt.setInt(1, quantity);
                stmt.setInt(2, selectedEvent.getId());
                stmt.executeUpdate();
                System.out.println("Database updated: soldTickets + " + quantity);
            } catch (SQLException e) {
                e.printStackTrace();
                statusLabel.setText("Error updating ticket count.");
                return;
            }

            // Refresh the main event table
            if (eventController != null) {
                eventController.loadEventsFromDB();
                System.out.println("Event table refreshed after cart addition.");
            }

            // Close the popup window
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
            System.out.println("Ticket popup window closed.");

            statusLabel.setText(quantity + " tickets added to cart.");

        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter a valid number.");
            System.out.println("Failed to parse ticket quantity: " + quantityField.getText());
        }
    }

    /**
     * Helper method to check if booking is allowed based on today’s date vs event day.
     * Booking is not allowed for days that have already passed in the week.
     */
    private boolean isBookingAllowedToday(String eventDay) {
        List<String> daysOfWeek = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

        String todayShort = LocalDate.now()
                .getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        int todayIndex = daysOfWeek.indexOf(todayShort);
        int eventIndex = daysOfWeek.indexOf(eventDay);

        System.out.println("Today is " + todayShort + " (index " + todayIndex + "), Event is on " + eventDay + " (index " + eventIndex + ")");

        return eventIndex >= todayIndex;
    }
}
