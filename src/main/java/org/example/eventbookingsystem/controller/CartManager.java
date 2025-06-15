package org.example.eventbookingsystem.controller;

import org.example.eventbookingsystem.model.CartItem;
import org.example.eventbookingsystem.utilities.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CartManager {

    // Singleton instance of CartManager (only one will exist in the app)
    private static CartManager instance;

    // Stores carts mapped by usernames (each user has their own cart list)
    private final Map<String, List<CartItem>> userCarts = new HashMap<>();

    // Private constructor to enforce Singleton
    private CartManager() {}

    // This ensures only one CartManager is ever created
    public static CartManager getInstance() {
        if (instance == null) {
            System.out.println("Creating a fresh instance of CartManager.");
            instance = new CartManager();
        }
        return instance;
    }

    // Adds an item to a user's cart, increasing quantity if it already exists
    public void addToCart(String username, CartItem item) {
        System.out.println("Adding item to cart for user: " + username);
        List<CartItem> cart = userCarts.computeIfAbsent(username, k -> new ArrayList<>());

        for (CartItem existingItem : cart) {
            if (existingItem.getEvent().getId() == item.getEvent().getId()) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                System.out.println("Increased quantity for existing item in cart.");
                return;
            }
        }

        cart.add(item);
        System.out.println("Item added as new entry in cart.");
    }

    // Fetches the cart items for a specific user
    public List<CartItem> getCartItems(String username) {
        return userCarts.getOrDefault(username, new ArrayList<>());
    }

    // Clears the user's cart completely
    public void clearCart(String username) {
        userCarts.remove(username);
        System.out.println("Cart cleared for user: " + username);
    }

    // Calculates the total bill of the user's cart
    public double getTotalAmount(String username) {
        return getCartItems(username).stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();
    }

    // Finalizes the cart as an order, adds it to the database, and clears the cart
    public void checkout(String username) {
        System.out.println("Starting checkout process for: " + username);
        List<CartItem> cartItems = getCartItems(username);

        try (Connection conn = DBUtil.getConnection()) {
            for (CartItem item : cartItems) {
                String insert = "INSERT INTO orders (orderNumber, username, eventName, venue, day, quantity, totalPrice, dateTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(insert);
                stmt.setString(1, generateOrderNumber(conn));
                stmt.setString(2, username);
                stmt.setString(3, item.getEvent().getName());
                stmt.setString(4, item.getEvent().getVenue());
                stmt.setString(5, item.getEvent().getDay());
                stmt.setInt(6, item.getQuantity());
                stmt.setDouble(7, item.getTotalPrice());
                stmt.setString(8, getCurrentTimestamp());
                stmt.executeUpdate();
                System.out.println("Order placed for event: " + item.getEvent().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        clearCart(username);
        System.out.println("Checkout complete. Cart is now empty.");
    }

    // Updates quantity of tickets in cart and reflects that change in the database
    public void updateCartItemQuantity(String username, int eventId, int newQuantity) {
        List<CartItem> cart = getCartItems(username);
        for (CartItem item : cart) {
            if (item.getEvent().getId() == eventId) {
                int difference = newQuantity - item.getQuantity();
                updateSoldTickets(eventId, difference);
                item.setQuantity(newQuantity);
                System.out.println("Updated quantity for event ID " + eventId + " to " + newQuantity);
                return;
            }
        }
    }

    // Updates the sold ticket count in the database for a given event
    private void updateSoldTickets(int eventId, int updatedvalue) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE events SET soldTickets = soldTickets + ? WHERE id = ?")) {
            stmt.setInt(1, updatedvalue);
            stmt.setInt(2, eventId);
            stmt.executeUpdate();
            System.out.println("Updated soldTickets by " + updatedvalue + " for event ID " + eventId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Removes a specific event from the user's cart and rolls back sold ticket count
    public void removeFromCart(String username, int eventId) {
        List<CartItem> cart = getCartItems(username);

        Iterator<CartItem> iterator = cart.iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getEvent().getId() == eventId) {
                int quantityToRemove = item.getQuantity();

                iterator.remove();
                System.out.println("Removed event ID " + eventId + " from user cart.");

                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE events SET soldTickets = soldTickets - ? WHERE id = ?")) {

                    stmt.setInt(1, quantityToRemove);
                    stmt.setInt(2, eventId);
                    stmt.executeUpdate();
                    System.out.println("Rolled back " + quantityToRemove + " tickets for event ID " + eventId);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                break;
            }
        }
    }

    // Generates a unique order number based on the max existing number
    public String generateOrderNumber(Connection conn) {
        String query = "SELECT MAX(CAST(orderNumber AS INTEGER)) AS maxOrder FROM orders";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next() && rs.getString("maxOrder") != null) {
                int next = Integer.parseInt(rs.getString("maxOrder")) + 1;
                return String.format("%04d", next);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return "0001"; // Default order number if no orders exist yet
    }

    // Returns the current timestamp in formatted style
    public String getCurrentTimestamp() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Set<String> getAllUsers() {
        return userCarts.keySet();
    }

    public void removeItemFromCartByComposite(String username, CartItem item) {
        List<CartItem> cart = userCarts.get(username);
        if (cart != null) {
            cart.remove(item);
        }
    }
}
