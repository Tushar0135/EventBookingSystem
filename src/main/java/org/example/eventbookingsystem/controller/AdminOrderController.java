package org.example.eventbookingsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.eventbookingsystem.model.Order;
import org.example.eventbookingsystem.utilities.DBUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class AdminOrderController {

    // Table and column bindings for the admin's full order view
    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, String> usernameColumn;
    @FXML private TableColumn<Order, String> orderNumberColumn;
    @FXML private TableColumn<Order, String> eventNameColumn;
    @FXML private TableColumn<Order, String> venueColumn;
    @FXML private TableColumn<Order, String> dayColumn;
    @FXML private TableColumn<Order, Integer> quantityColumn;
    @FXML private TableColumn<Order, Double> totalPriceColumn;
    @FXML private TableColumn<Order, String> dateTimeColumn;

    /**
     * Called automatically when the Admin Order window is loaded.
     * This method binds table columns to order model fields and fetches order data from the database.
     */
    @FXML
    public void initialize() {
        System.out.println("AdminOrderController: Initializing the Admin Order window...");

        // Binding each column in the table to corresponding Order properties
        usernameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("username"));
        orderNumberColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("orderNumber"));
        eventNameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("eventName"));
        venueColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("venue"));
        dayColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("day"));
        quantityColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("quantity"));
        totalPriceColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("totalPrice"));
        dateTimeColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateTime"));

        // Now load all the orders from the database into the table
        loadAllOrders();
    }

    /**
     * Fetches all orders from the database and populates them in the admin's order table.
     */
    private void loadAllOrders() {
        System.out.println("Fetching all order records from the database...");
        String query = "SELECT * FROM orders ORDER BY dateTime DESC";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Creating Order objects from the result set and adding them to the list
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

            // Displaying orders in the UI table
            orderTable.getItems().setAll(orders);
            System.out.println("Loaded " + orders.size() + " orders into the admin order table.");

        } catch (SQLException e) {
            System.out.println("Error while loading orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Closes the current order view window when the admin clicks on the "Close" button.
     */
    @FXML
    private void handleClose() {
        System.out.println("Admin clicked Close - closing the Admin Order window.");
        Stage stage = (Stage) orderTable.getScene().getWindow();
        stage.close();
    }
    /**
     * Handles the export button click — allows admin to save the order list to a text file.
     */
    @FXML
    private void handleExportAllOrders() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Orders to File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        fileChooser.setInitialFileName("all_orders.txt");

        File selectedFile = fileChooser.showSaveDialog(orderTable.getScene().getWindow());

        if (selectedFile != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile))) {
                for (Order order : orderTable.getItems()) {
                    writer.write("Order Number: " + order.getOrderNumber());
                    writer.newLine();
                    writer.write("Username: " + order.getUsername());
                    writer.newLine();
                    writer.write("Event: " + order.getEventName());
                    writer.newLine();
                    writer.write("Venue: " + order.getVenue());
                    writer.newLine();
                    writer.write("Day: " + order.getDay());
                    writer.newLine();
                    writer.write("Quantity: " + order.getQuantity());
                    writer.newLine();
                    writer.write("Total Price: $" + String.format("%.2f", order.getTotalPrice()));
                    writer.newLine();
                    writer.write("Date & Time: " + order.getDateTime());
                    writer.newLine();
                    writer.write("--------------------------------------------------");
                    writer.newLine();
                }

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Export Complete");
                success.setHeaderText(null);
                success.setContentText("All orders have been exported successfully.");
                success.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Export Failed");
                error.setHeaderText(null);
                error.setContentText("Could not export orders: " + e.getMessage());
                error.showAndWait();
            }
        }
    }

}
