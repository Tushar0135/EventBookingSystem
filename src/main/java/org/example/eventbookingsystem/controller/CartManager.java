package org.example.eventbookingsystem.controller;

import org.example.eventbookingsystem.model.CartItem;
import org.example.eventbookingsystem.model.Event;
import org.example.eventbookingsystem.utilities.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CartManager {

    private static CartManager instance;

    private final Map<String, List<CartItem>> userCarts = new HashMap<>();

    private CartManager() {
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addToCart(String username, CartItem item) {
        List<CartItem> cart = userCarts.computeIfAbsent(username, k -> new ArrayList<>());

        for (CartItem existingItem : cart) {
            if (existingItem.getEventName().equals(item.getEventName())
                    && existingItem.getVenue().equals(item.getVenue())
                    && existingItem.getEvent().getDay().equals(item.getEvent().getDay())) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                updateCartInDB(username, existingItem);
                return;
            }
        }

        // If not in memory, check DB
        if (getCartItemFromDB(username, item)) {
            for (CartItem dbItem : cart) {
                if (dbItem.getEventName().equals(item.getEventName())
                        && dbItem.getVenue().equals(item.getVenue())
                        && dbItem.getEvent().getDay().equals(item.getEvent().getDay())) {
                    dbItem.setQuantity(dbItem.getQuantity() + item.getQuantity());
                    updateCartInDB(username, dbItem);
                    return;
                }
            }
        }

        cart.add(item);
        insertCartToDB(username, item);
    }

    private boolean getCartItemFromDB(String username, CartItem item) {
        String sql = "SELECT quantity FROM cart WHERE username = ? AND event_name = ? AND event_venue = ? AND event_day = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, item.getEventName());
            stmt.setString(3, item.getVenue());
            stmt.setString(4, item.getEvent().getDay());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private int getEventIdFromEventsTable(String name, String venue, String day) {
        String query = "SELECT id FROM events WHERE name = ? AND venue = ? AND day = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setString(2, venue);
            stmt.setString(3, day);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void insertCartToDB(String username, CartItem item) {
        String query = "INSERT INTO cart (username, event_name, event_venue, event_day, event_price, quantity) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, item.getEvent().getName());
            stmt.setString(3, item.getEvent().getVenue());
            stmt.setString(4, item.getEvent().getDay());
            stmt.setDouble(5, item.getEvent().getPrice());
            stmt.setInt(6, item.getQuantity());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void updateCartInDB(String username, CartItem item) {
        String query = "UPDATE cart SET quantity = ? WHERE username = ? AND event_name = ? AND event_venue = ? AND event_day = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, item.getQuantity());
            stmt.setString(2, username);
            stmt.setString(3, item.getEventName());
            stmt.setString(4, item.getVenue());
            stmt.setString(5, item.getEvent().getDay());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<CartItem> getCartItems(String username) {
        // If not loaded, fetch from DB
        if (!userCarts.containsKey(username)) {
            loadCartFromDB(username);
        }
        return userCarts.getOrDefault(username, new ArrayList<>());
    }

    public void loadCartFromDB(String username) {
        List<CartItem> items = new ArrayList<>();
        String query = "SELECT * FROM cart WHERE username = ?";
        try (Connection conn = DBUtil.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString("event_name");
                String venue = rs.getString("event_venue");
                String day = rs.getString("event_day");
                double price = rs.getDouble("event_price");
                int quantity = rs.getInt("quantity");

                // ✅ Fetch actual eventId from the events table
                int eventId = getEventIdFromEventsTable(name, venue, day);

                Event event = new Event(eventId, name, venue, day, price, 0, 0, true);
                CartItem item = new CartItem(event, quantity, price);
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        userCarts.put(username, items);
    }


    public void clearCart(String username) {
        userCarts.remove(username);
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM cart WHERE username = ?")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getTotalAmount(String username) {
        return getCartItems(username).stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();
    }

    public void checkout(String username) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        clearCart(username);
    }

    public void updateCartItemQuantity(String username, String eventName, String venue, String day, int newQuantity) {
        List<CartItem> cart = getCartItems(username);
        for (CartItem item : cart) {
            if (item.getEventName().equals(eventName)
                    && item.getVenue().equals(venue)
                    && item.getEvent().getDay().equals(day)) {

                int eventId = item.getEvent().getId();
                int oldQty = item.getQuantity();
                int diff = newQuantity - oldQty;

                // Update memory
                item.setQuantity(newQuantity);

                // Update cart table
                updateCartInDB(username, item);

                // Update soldTickets in events table
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE events SET soldTickets = soldTickets + ? WHERE id = ?")) {
                    stmt.setInt(1, diff); // Can be + or -
                    stmt.setInt(2, eventId);
                    stmt.executeUpdate();
                    System.out.println("Updated soldTickets in DB by " + diff + " for eventId: " + eventId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }
        }
    }

    public void removeFromCart(String username, String eventName, String venue, String day) {
        List<CartItem> cart = getCartItems(username);
        Iterator<CartItem> iterator = cart.iterator();

        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getEventName().equals(eventName)
                    && item.getVenue().equals(venue)
                    && item.getEvent().getDay().equals(day)) {

                int eventId = item.getEvent().getId(); // Ensure Event has ID set
                int quantity = item.getQuantity();

                // Step 1: Remove from memory
                iterator.remove();

                // Step 2: Remove from cart table in DB
                String query = "DELETE FROM cart WHERE username = ? AND event_name = ? AND event_venue = ? AND event_day = ?";
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, eventName);
                    stmt.setString(3, venue);
                    stmt.setString(4, day);
                    stmt.executeUpdate();
                    System.out.println("Removed item from DB: " + eventName + " | " + venue + " | " + day);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                // Step 3: Update soldTickets in events table (subtract quantity)
                if (eventId > 0) {
                    try (Connection conn = DBUtil.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(
                                 "UPDATE events SET soldTickets = soldTickets - ? WHERE id = ?")) {
                        stmt.setInt(1, quantity);
                        stmt.setInt(2, eventId);
                        stmt.executeUpdate();
                        System.out.println("Updated soldTickets for eventId: " + eventId + " (-" + quantity + ")");
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Warning: Event ID is missing. Cannot update soldTickets.");
                }

                break; // Exit after removing one matching item
            }
        }
    }

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
        return "0001";
    }

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

    public void removeEventFromAllCarts(String eventName, String venue, String day) {
        System.out.println("CartManager called to remove event: " + eventName + " | " + venue + " | " + day);
        System.out.println("Active user carts: " + userCarts.keySet());
        for (String username : userCarts.keySet()) {
            List<CartItem> cart = userCarts.get(username);
            Iterator<CartItem> iterator = cart.iterator();
            System.out.println("CartManager called to remove event: " + eventName + " | " + venue + " | " + day);
            System.out.println("Active user carts: " + userCarts.keySet());
            while (iterator.hasNext()) {
                CartItem item = iterator.next();
                if (item.getEventName().equals(eventName)
                        && item.getVenue().equals(venue)
                        && item.getEvent().getDay().equals(day)) {

                    int qty = item.getQuantity();
                    int eventId = item.getEvent().getId();

                    // Remove from memory
                    iterator.remove();

                    // Remove from DB
                    try (Connection conn = DBUtil.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("DELETE FROM cart WHERE  event_name = ? AND event_venue = ? AND event_day = ?")) {
                        System.out.println("Attempting to delete from DB: " + eventName + " | " + venue + " | " + day);
                        stmt.setString(1, eventName);
                        stmt.setString(2, venue);
                        stmt.setString(3, day);
                        stmt.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Update soldTickets
                    try (Connection conn = DBUtil.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("UPDATE events SET soldTickets = soldTickets - ? WHERE id = ?")) {
                        stmt.setInt(1, qty);
                        stmt.setInt(2, eventId);
                        stmt.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.println("Disabled event removed from cart of user: " + username);
                }
            }
        }
    }

    public void preloadAllUserCarts() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT username FROM cart")) {

            while (rs.next()) {
                String username = rs.getString("username");
                loadCartForUser(username);
            }

            System.out.println("All user carts preloaded into memory. Total: " + userCarts.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void loadCartForUser(String username) {
        List<CartItem> cart = new ArrayList<>();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM cart WHERE username = ?")) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String eventName = rs.getString("event_name");
                String venue = rs.getString("event_venue");
                String day = rs.getString("event_day");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("event_price");
                int eventId = rs.getInt("id");

                // Construct a dummy Event object to attach to the CartItem
                Event event = new Event(eventId, eventName, venue, day, price, 0, 0, true);
                CartItem item = new CartItem(event, quantity, price);
                cart.add(item);
            }

            if (!cart.isEmpty()) {
                userCarts.put(username, cart);
                System.out.println("Loaded cart for user: " + username + ", items: " + cart.size());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

