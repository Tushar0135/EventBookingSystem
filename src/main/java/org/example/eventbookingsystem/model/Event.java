package org.example.eventbookingsystem.model;

/**
 * The Event class represents an event in the booking system.
 * It contains all necessary details like name, venue, day, price, total and sold tickets.
 */
public class Event {
    private int id;
    private String name;
    private String venue;
    private String day;
    private double price;
    private int soldTickets;
    private int totalTickets;
    private String status;
    private boolean enabled;

    /**
     * Constructs an Event with all required properties.
     */
    public Event(int id, String name, String venue, String day, double price, int soldTickets, int totalTickets, boolean enabled) {
        this.id = id;
        this.name = name;
        this.venue = venue;
        this.day = day;
        this.price = price;
        this.soldTickets = soldTickets;
        this.totalTickets = totalTickets;
        this.enabled = enabled;

        System.out.println("Created new event: " + name + " at " + venue + " on " + day);
    }

    // Getter methods for all fields used for UI or logic
    public String getName() {
        return name;
    }

    public String getVenue() {
        return venue;
    }

    public String getDay() {
        return day;
    }

    public double getPrice() {
        return price;
    }

    public int getSoldTickets() {
        return soldTickets;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    /**
     * Calculates tickets still available.
     */
    public int getAvailableTickets() {
        return totalTickets - soldTickets;
    }

    // Setters to update event properties dynamically

    public void setSoldTickets(int soldTickets) {
        this.soldTickets = soldTickets;
        System.out.println("Updated sold tickets for event '" + name + "' to: " + soldTickets);
    }

    public void setTotalTickets(int totalTickets) {
        this.totalTickets = totalTickets;
        System.out.println("Updated total tickets for event '" + name + "' to: " + totalTickets);
    }

    public void setName(String name) {
        this.name = name;
        System.out.println("Updated event name to: " + name);
    }

    public void setVenue(String venue) {
        this.venue = venue;
        System.out.println("Updated venue for event '" + name + "' to: " + venue);
    }

    public void setDay(String day) {
        this.day = day;
        System.out.println("Updated day for event '" + name + "' to: " + day);
    }

    public void setPrice(double price) {
        this.price = price;
        System.out.println("Updated ticket price for event '" + name + "' to: $" + price);
    }

    public int getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        System.out.println("Updated status for event '" + name + "' to: " + status);
    }
}
