package org.example.eventbookingsystem.model;

/**
 * CartItem represents an individual booking of tickets for an event.
 * It holds the selected event, quantity of tickets booked, and the event price.
 */
public class CartItem {
    private Event event;
    private int quantity;
    private double price;

    /**
     * Constructs a CartItem object with a specific event, quantity, and price.
     */
    public CartItem(Event event, int quantity, double price) {
        this.event = event;
        this.quantity = quantity;
        this.price = price;
        System.out.println("Created CartItem for event: " + event.getName() + ", Quantity: " + quantity + ", Price per ticket: " + price);
    }

    /**
     * Returns the associated event object.
     */
    public Event getEvent() {
        return event;
    }

    /**
     * Returns the price per ticket (stored separately for flexibility).
     */
    public double getPrice() {
        return price;
    }

    /**
     * Returns how many tickets were added for this event.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Calculates the total cost for this cart item (price * quantity).
     */
    public double getTotalPrice() {
        return event.getPrice() * quantity;
    }

    /**
     * Returns the event name — used for UI display.
     */
    public String getEventName() {
        return event.getName();
    }

    /**
     * Returns the venue for the event — used for UI display.
     */
    public String getVenue() {
        return event.getVenue();
    }

    /**
     * Updates the number of tickets in the cart item.
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        System.out.println("Updated ticket quantity for event: " + event.getName() + " to " + quantity);
    }
    public String getDay() {
        return event.getDay();
    }

}
