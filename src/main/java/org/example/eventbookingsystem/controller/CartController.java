package org.example.eventbookingsystem.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.eventbookingsystem.model.CartItem;
import org.example.eventbookingsystem.model.Event;
import org.example.eventbookingsystem.model.User;
import org.example.eventbookingsystem.utilities.DBUtil;
import org.example.eventbookingsystem.utilities.Session;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CartController {
    @FXML
    private VBox root;
    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private TableColumn<CartItem, String> eventNameColumn;
    @FXML
    private TableColumn<CartItem, String> venueColumn;
    @FXML
    private TableColumn<CartItem, Integer> quantityColumn;
    @FXML
    private TableColumn<CartItem, Double> totalPriceColumn;
    @FXML
    private TableColumn<CartItem, Double> priceColumn;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private Button goToEventsButton;
    @FXML
    private TableColumn<CartItem, String> dayColumn;
    private EventController eventController;

    // This sets the EventController reference, allowing cart updates to reflect in the event list
    public void setEventController(EventController eventController) {
        this.eventController = eventController;
    }

    // Initializes the cart page UI when it loads
    @FXML
    public void initialize() {
        System.out.println("Cart page initialized. Setting up the cart table...");

        eventNameColumn.setCellValueFactory(new PropertyValueFactory<>("eventName"));
        venueColumn.setCellValueFactory(new PropertyValueFactory<>("venue"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        totalPriceColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("day"));

        String username = Session.getLoggedInUsername();

        CartManager.getInstance().loadCartFromDB(username); // <- Ensures sync
        cartTable.getItems().setAll(CartManager.getInstance().getCartItems(username));

        double totalAmount = CartManager.getInstance().getTotalAmount(username);
        totalAmountLabel.setText(String.format("Total: $%.2f", totalAmount));
    }

    private void loadCartFromDB(String username) {
        String query = "SELECT * FROM cart WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("event_name");
                String venue = rs.getString("event_venue");
                String day = rs.getString("event_day");
                double price = rs.getDouble("event_price");
                int quantity = rs.getInt("quantity");

                Event dummyEvent = new Event(0, name, venue, day, price, 0, 0, true);
                CartItem item = new CartItem(dummyEvent, quantity, price);
                CartManager.getInstance().addToCart(username, item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            System.out.println("Quantity update initiated for event: " + selectedItem.getEvent().getName());

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

                    // ✅ Let CartManager handle full update
                    CartManager.getInstance().updateCartItemQuantity(
                            Session.getLoggedInUsername(),
                            selectedItem.getEventName(),
                            selectedItem.getVenue(),
                            selectedItem.getEvent().getDay(),
                            newQty
                    );

                    // ✅ Refresh Cart UI
                    cartTable.getItems().clear();
                    cartTable.getItems().addAll(CartManager.getInstance().getCartItems(Session.getLoggedInUsername()));
                    totalAmountLabel.setText(String.format("Total: $%.2f", CartManager.getInstance().getTotalAmount(Session.getLoggedInUsername())));

                    if (eventController != null) {
                        eventController.loadEventsFromDB();
                    }

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

    // Removes an item from the cart
    @FXML
    private void handleRemoveItem() {
        CartItem selectedItem = cartTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            System.out.println("Removing item: " + selectedItem.getEvent().getName() + " from cart.");

            String username = Session.getLoggedInUsername();

            // ✅ Only call the manager method to ensure centralized handling
            CartManager.getInstance().removeFromCart(
                    username,
                    selectedItem.getEventName(),
                    selectedItem.getVenue(),
                    selectedItem.getEvent().getDay()
            );

            // ✅ Refresh frontend UI
            cartTable.getItems().clear();
            cartTable.getItems().addAll(CartManager.getInstance().getCartItems(username));
            totalAmountLabel.setText(String.format("Total: $%.2f", CartManager.getInstance().getTotalAmount(username)));

            if (eventController != null) {
                eventController.loadEventsFromDB(); // Refresh event list
            }

            System.out.println("Item successfully removed from all layers.");
        } else {
            showAlert("Please select an item to remove.");
        }
    }

}
