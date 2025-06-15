// Unit test class to validate critical functionalities of the event booking system
import org.example.eventbookingsystem.model.Event;
import org.example.eventbookingsystem.model.CartItem;
import org.example.eventbookingsystem.utilities.*;
import org.example.eventbookingsystem.controller.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class testcases {

    // This test ensures that the encryption function always produces the same output
    // for the same input. Useful to ensure password hashing is consistent.
    @Test
    public void testEncryptionConsistency() {
        String input = "password123";
        String encrypted1 = PasswordEncryption.encryptPassword(input);
        String encrypted2 = PasswordEncryption.encryptPassword(input);
        assertEquals(encrypted1, encrypted2, "Encryption should be deterministic and consistent");
    }

    // This test ensures that the encrypted password is different from the original input
    // to verify that plain text passwords are not stored.
    @Test
    public void testEncryptionNotPlainText() {
        String input = "password123";
        String encrypted = PasswordEncryption.encryptPassword(input);
        assertNotEquals(input, encrypted, "Encrypted password should not match plain text");
    }

    // This test adds an event to a user's cart and then checks if it was successfully stored.
    @Test
    public void testAddToCartAndRetrieve() {
        CartManager manager = CartManager.getInstance();
        Event event = new Event(1, "Concert", "Hall A", "Sat", 20.0, 0, 100, true);
        CartItem item = new CartItem(event, 2, event.getPrice());
        manager.addToCart("testUser", item);

        List<CartItem> items = manager.getCartItems("testUser");
        assertEquals(1, items.size());
        assertEquals("Concert", items.get(0).getEventName());
    }

    // This test validates that the cart total is correctly calculated based on item quantity and price.
    @Test
    public void testCartTotalCalculation() {
        CartManager manager = CartManager.getInstance();
        Event event = new Event(2, "Show", "Auditorium", "Sun", 15.0, 0, 50, true);
        CartItem item = new CartItem(event, 3, event.getPrice());
        manager.addToCart("calcUser", item);

        double total = manager.getTotalAmount("calcUser");
        assertEquals(45.0, total);
    }

    // This test updates the quantity of an item in the cart and checks if the change is reflected.
    @Test
    void testUpdateCartItemQuantity() {
        CartManager manager = CartManager.getInstance();
        Event event = new Event(2, "Drama", "Stage A", "Fri", 30.0, 0, 50, true);
        CartItem item = new CartItem(event, 1, event.getPrice());
        String user = "updateUser";

        manager.addToCart(user, item);
        manager.updateCartItemQuantity(user, event.getId(), 4);

        List<CartItem> updated = manager.getCartItems(user);
        assertEquals(4, updated.get(0).getQuantity(), "Quantity should be updated to 4.");
    }

    // This test adds two events to the cart and ensures the total is calculated as expected.
    @Test
    void testTotalAmountCalculation() {
        CartManager manager = CartManager.getInstance();
        String user = "priceUser";

        Event e1 = new Event(4, "Play", "Main Hall", "Wed", 50.0, 0, 80, true);
        Event e2 = new Event(5, "Talk Show", "Stage B", "Thu", 40.0, 0, 70, true);

        manager.addToCart(user, new CartItem(e1, 2, e1.getPrice())); // Total = 100
        manager.addToCart(user, new CartItem(e2, 1, e2.getPrice())); // Total = 40

        assertEquals(140.0, manager.getTotalAmount(user), 0.01, "Total amount should be correct");
    }

    // This test ensures the cart is properly cleared for a user.
    @Test
    void testClearCart() {
        CartManager manager = CartManager.getInstance();
        String user = "clearUser";

        Event e = new Event(6, "Fair", "Park", "Mon", 10.0, 0, 100, true);
        manager.addToCart(user, new CartItem(e, 5, e.getPrice()));

        manager.clearCart(user);
        assertTrue(manager.getCartItems(user).isEmpty(), "Cart should be cleared");
    }

    // This test ensures that an empty cart returns a total amount of zero.
    @Test
    public void testGetTotalAmount_EmptyCartReturnsZero() {
        CartManager manager = CartManager.getInstance();
        manager.clearCart("emptyUser");

        double total = manager.getTotalAmount("emptyUser");
        assertEquals(0.0, total, 0.01);
    }
}
