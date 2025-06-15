package org.example.eventbookingsystem.model;

/**
 * GroupedItem represents a simplified view of an event,
 * grouping venue and day information together, and tracking its ID and enabled status.
 * This is mainly used in admin views where events are grouped for management purposes.
 */
public class GroupedItem {
    private final String eventName;
    private final String venueDay;
    private final int eventId;
    private final boolean enabled;

    /**
     * Constructs a grouped item for admin listing.
     *
     * @param eventName the name of the event
     * @param venueDay  combination of venue and day (e.g., "Auditorium - Friday")
     * @param eventId   the unique ID of the event
     * @param enabled   true if the event is active; false if disabled
     */
    public GroupedItem(String eventName, String venueDay, int eventId, boolean enabled) {
        this.eventName = eventName;
        this.venueDay = venueDay;
        this.eventId = eventId;
        this.enabled = enabled;

        System.out.println("GroupedItem created for event: " + eventName +
                ", Venue-Day: " + venueDay +
                ", Status: " + (enabled ? "Enabled" : "Disabled"));
    }

    // Get the name of the event
    public String getEventName() {
        return eventName;
    }

    // Get the combined venue and day string
    public String getVenueDay() {
        return venueDay;
    }

    // Get the unique identifier of the event
    public int getEventId() {
        return eventId;
    }

    /**
     * Returns a user-friendly status label ("Enabled"/"Disabled")
     */
    public String getStatus() {
        return enabled ? "Enabled" : "Disabled";
    }

    // Returns the boolean status directly (used for internal logic)
    public boolean isEnabled() {
        return enabled;
    }
}
