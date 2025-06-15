package org.example.eventbookingsystem.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.eventbookingsystem.model.CartItem;
import org.example.eventbookingsystem.utilities.DBUtil;
import org.example.eventbookingsystem.utilities.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CartController {
    @FXML private VBox root;
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> eventNameColumn;
    @FXML private TableColumn<CartItem, String> venueColumn;
    @FXML private TableColumn<CartItem, Integer> quantityColumn;
    @FXML private TableColumn<CartItem, Double> totalPriceColumn;
    @FXML private TableColumn<CartItem, Double> priceColumn;
    @FXML private Label totalAmountLabel;
    @FXML private TableColumn<CartItem, String> dayColumn;
    private EventController eventController;

    // This sets the EventController reference, allowing cart updates to reflect in the event list
    public void setEventController(EventController eventController) {
        this.eventController = eventController;
    }

    // Initializes the cart page UI when it loads
    @FXML
    public void initialize() {
        System.out.println("Cart page initialized. Setting up the cart table...");

        // Linking table columns to CartItem properties
        eventNameColumn.setCellValueFactory(new PropertyValueFactory<>("eventName"));
        venueColumn.setCellValueFactory(new PropertyValueFactory<>("venue"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        totalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));

        String username = Session.getLoggedInUsername();
        cartTable.getItems().setAll(CartManager.getInstance().getCartItems(username));
        double totalAmount = CartManager.getInstance().getTotalAmount(username);
        totalAmountLabel.setText(String.format("Total: $%.2f", totalAmount));

        System.out.println("Cart loaded for user: " + username + " | Total amount: $" + totalAmount);
    }

    // Called when the user clicks the "Checkout" button
    @FXML
    public void handleCheckout() {
        String username = Session.getLoggedInUsername();
        if (CartManager.getInstance().getCartItems(username).isEmpty()) {
            System.out.println("Checkout attempted with an empty cart.");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Checkout");
            alert.setHeaderText(null);
            alert.setContentText("Your cart is empty!");
            alert.showAndWait();
            return;
        }

        double total = CartManager.getInstance().getTotalAmount(username);
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Checkout");
        confirmAlert.setHeaderText("Total Amount: $" + String.format("%.2f", total));
        confirmAlert.setContentText("Do you want to proceed to payment?");
        System.out.println("Checkout initiated. Awaiting user confirmation...");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("User confirmed checkout. Prompting for confirmation code...");

                TextInputDialog codeDialog = new TextInputDialog();
                codeDialog.setTitle("Enter Confirmation Code");
                codeDialog.setHeaderText("A 6-digit code has been sent to your phone/email.");
                codeDialog.setContentText("Enter the 6-digit code:");

                codeDialog.showAndWait().ifPresent(code -> {
                    if (code.matches("\\d{6}")) {
                        System.out.println(" Confirmation code accepted. Proceeding to checkout...");
                        CartManager.getInstance().checkout(username);
                        cartTable.getItems().clear();
                        totalAmountLabel.setText("Total: $0.00");

                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Checkout");
                        successAlert.setHeaderText(null);
                        successAlert.setContentText("Checkout successful!");
                        successAlert.showAndWait();

                        if (eventController != null) {
                            eventController.loadEventsFromDB();
                        }
                    } else {
                        System.out.println(" Invalid confirmation code entered.");
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Invalid Code");
                        errorAlert.setHeaderText(null);
                        errorAlert.setContentText("Please enter a valid 6-digit numeric code.");
                        errorAlert.showAndWait();
                    }
                });
            }
        });
    }

    // Closes the cart window when "Close" is clicked
    @FXML
    private void handleClose() {
        System.out.println(" Closing the cart window...");
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }

    // Handles quantity update for a selected item in the cart
    @FXML
    private void handleUpdateQuantity() {
        CartItem selectedItem = cartTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            System.out.println(" Quantity update initiated for event: " + selectedItem.getEvent().getName());

            TextInputDialog dialog = new TextInputDialog(String.valueOf(selectedItem.getQuantity()));
            dialog.setTitle("Update Quantity");
            dialog.setHeaderText("Update quantity for: " + selectedItem.getEvent().getName());
            dialog.setContentText("Enter new quantity:");

            dialog.showAndWait().ifPresent(input -> {
                try {
                    int newQty = Integer.parseInt(input);
                    if (newQty <= 0) {
                        showAlert("Quantity must be greater than 0.");
                        return;
                    }

                    int available = getAvailableTicketsFromDB(selectedItem.getEvent().getId());
                    int currentQty = selectedItem.getQuantity();
                    int extraNeeded = newQty - currentQty;

                    if (extraNeeded > available) {
                        showAlert("Only " + available + " tickets available. Reduce your quantity.");
                        return;
                    }

                    CartManager.getInstance().updateCartItemQuantity(
                            Session.getLoggedInUsername(),
                            selectedItem.getEvent().getId(),
                            newQty
                    );

                    cartTable.refresh();
                    totalAmountLabel.setText(String.format("Total: $%.2f", CartManager.getInstance().getTotalAmount(Session.getLoggedInUsername())));
                    eventController.loadEventsFromDB();

                    System.out.println("Quantity updated to " + newQty + " for event: " + selectedItem.getEvent().getName());
                } catch (NumberFormatException e) {
                    showAlert("Invalid number entered.");
                }
            });
        }
    }

    // Fetches the number of tickets left for an event from the database
    private int getAvailableTicketsFromDB(int eventId) {
        String query = "SELECT totalTickets, soldTickets FROM events WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("totalTickets") - rs.getInt("soldTickets");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Utility method to show a warning alert
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Quantity");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Removes an item from the cart
    @FXML
    private void handleRemoveItem() {
        CartItem selectedItem = cartTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            System.out.println("Removing item: " + selectedItem.getEvent().getName() + " from cart.");

            // Remove from cart memory
            CartManager.getInstance().removeFromCart(Session.getLoggedInUsername(), selectedItem.getEvent().getId());

            // Refresh table
            cartTable.getItems().remove(selectedItem);
            cartTable.getItems().clear();
            cartTable.getItems().addAll(CartManager.getInstance().getCartItems(Session.getLoggedInUsername()));

            // Update total label
            totalAmountLabel.setText(String.format("Total: $%.2f",
                    CartManager.getInstance().getTotalAmount(Session.getLoggedInUsername())));

            // Refresh event list if available
            if (eventController != null) {
                eventController.loadEventsFromDB();
            }

            System.out.println("Item removed and cart updated.");
        }
    }
}
