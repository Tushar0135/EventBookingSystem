package org.example.eventbookingsystem.model;

/**
 * The Order class represents a user's confirmed ticket booking.
 * It stores details such as order number, username, event info, quantity, and total cost.
 * This is used across both user and admin views for displaying order history and analytics.
 */
public class Order {
    private final String orderNumber;  // Unique identifier for the order
    private final String username;     // Username of the person who placed the order
    private final String eventName;    // Name of the event booked
    private final String venue;        // Venue of the event
    private final String day;          // Day on which the event will be held
    private final int quantity;        // Number of tickets purchased
    private final double totalPrice;   // Total price paid (price per ticket * quantity)
    private final String dateTime;     // Date and time the order was placed

    /**
     * Constructor to create a complete order object with all necessary details.
     *
     * @param orderNumber unique order ID
     * @param username user who made the booking
     * @param eventName event name
     * @param venue event venue
     * @param day event day
     * @param quantity number of tickets
     * @param totalPrice total amount charged
     * @param dateTime timestamp of order
     */
    public Order(String orderNumber, String username, String eventName, String venue, String day,
                 int quantity, double totalPrice, String dateTime) {
        this.orderNumber = orderNumber;
        this.username = username;
        this.eventName = eventName;
        this.venue = venue;
        this.day = day;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.dateTime = dateTime;

        // This is a handy point to log the order creation for debugging or confirmation.
        System.out.println("New Order Created: Order#" + orderNumber + " by user " + username + " for event " + eventName);
    }

    // Standard getters for use in TableViews, export, etc.

    public String getOrderNumber() { return orderNumber; }

    public String getUsername() { return username; }

    public String getEventName() { return eventName; }

    public String getVenue() { return venue; }

    public String getDay() { return day; }

    public int getQuantity() { return quantity; }

    public double getTotalPrice() { return totalPrice; }

    public String getDateTime() { return dateTime; }
}
