package org.example.eventbookingsystem.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.eventbookingsystem.model.Order;
import org.example.eventbookingsystem.model.User;
import org.example.eventbookingsystem.utilities.Session;
import org.example.eventbookingsystem.utilities.DBUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryController {

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> orderNumberColumn;
    @FXML private TableColumn<Order, String> eventNameColumn;
    @FXML private TableColumn<Order, String> venueColumn;
    @FXML private TableColumn<Order, String> dayColumn;
    @FXML private TableColumn<Order, Integer> quantityColumn;
    @FXML private TableColumn<Order, Double> totalPriceColumn;
    @FXML private TableColumn<Order, String> dateTimeColumn;
    @FXML
    private Button goToEventsButton;
    /**
     * Called automatically when this screen is loaded.
     * It sets up the columns and loads the current user's orders into the table.
     */
    @FXML
    public void initialize() {
        // Linking table columns to Order properties
        orderNumberColumn.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        eventNameColumn.setCellValueFactory(new PropertyValueFactory<>("eventName"));
        venueColumn.setCellValueFactory(new PropertyValueFactory<>("venue"));
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        totalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));

        // Get the username of the logged-in user
        String currentUsername = Session.getLoggedInUsername();
        System.out.println("Loading order history for: " + currentUsername);

        // Fetch and display orders
        orderTable.getItems().setAll(getOrdersByUsername(currentUsername));
    }

    /**
     * Retrieves all orders placed by a specific user from the database.
     */
    public static List<Order> getOrdersByUsername(String username) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE username = ? ORDER BY dateTime DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = new Order(
                        rs.getString("orderNumber"),
                        rs.getString("username"),
                        rs.getString("eventName"),
                        rs.getString("venue"),
                        rs.getString("day"),
                        rs.getInt("quantity"),
                        rs.getDouble("totalPrice"),
                        rs.getString("dateTime")
                );
                orders.add(order);
            }

            System.out.println("Fetched " + orders.size() + " orders from the database for user: " + username);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

    /**
     * Retrieves all orders from the database (used for admin panel).
     */
    public static List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY dateTime DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Order order = new Order(
                        rs.getString("orderNumber"),
                        rs.getString("username"),
                        rs.getString("eventName"),
                        rs.getString("venue"),
                        rs.getString("day"),
                        rs.getInt("quantity"),
                        rs.getDouble("totalPrice"),
                        rs.getString("dateTime")
                );
                orders.add(order);
            }

            System.out.println("Admin fetched " + orders.size() + " total orders from the database.");

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return orders;
    }

    /**
     * Handles the export button click — allows user to save the order list to a text file.
     */
    @FXML
    private void handleExportOrders() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Orders to Text File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        fileChooser.setInitialFileName("order_history.txt");

        File selectedFile = fileChooser.showSaveDialog(orderTable.getScene().getWindow());

        if (selectedFile != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
                List<Order> orders = orderTable.getItems();

                for (Order order : orders) {
                    writer.write("Order Number: " + order.getOrderNumber());
                    writer.newLine();
                    writer.write("Date & Time: " + order.getDateTime());
                    writer.newLine();
                    writer.write("Event: " + order.getEventName() + " | Venue: " + order.getVenue() + " | Day: " + order.getDay());
                    writer.newLine();
                    writer.write("Quantity: " + order.getQuantity());
                    writer.newLine();
                    writer.write("Total Price: $" + String.format("%.2f", order.getTotalPrice()));
                    writer.newLine();
                    writer.write("---------------------------------------------");
                    writer.newLine();
                }

                System.out.println("Order history successfully exported to file: " + selectedFile.getAbsolutePath());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Success");
                alert.setHeaderText(null);
                alert.setContentText("Orders exported successfully!");
                alert.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to export orders: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export orders: " + e.getMessage());
                alert.showAndWait();
            }
        } else {
            System.out.println("User cancelled export operation.");
        }
    }
    @FXML
    private void handleGoToEvents(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/eventbookingsystem/events.fxml"));
            Parent root = loader.load();

            // Create a User object using session username
            String username = Session.getLoggedInUsername();
            User user = new User(username);  // Assuming User has a constructor User(String username)

            EventController controller = loader.getController();
            controller.setCurrentUser(user); // Correct type passed here
            controller.loadEventsFromDB();   // Optional refresh

            // Show the event page scene
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Event Dashboard");
            stage.show();

            System.out.println("Redirected to Events Page.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to redirect to Events Page.");
        }
    }
}
