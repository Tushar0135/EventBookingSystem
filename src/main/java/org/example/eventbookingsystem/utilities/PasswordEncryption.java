package org.example.eventbookingsystem.utilities;

/**
 * Utility class for encrypting passwords using a simple Caesar cipher-like technique.
 * This is mainly used to obfuscate passwords before storing them in the database.
 */
public class PasswordEncryption {

    /**
     * Encrypts the given password by shifting each character by 3 positions.
     * - Digits (0–9) are rotated (e.g., 9 becomes 2).
     * - Letters (A–Z, a–z) are rotated within their cases.
     * - Special characters are left unchanged.
     *
     * @param password The plain text password
     * @return Encrypted password
     */
    public static String encryptPassword(String password) {
        System.out.println("Encrypting password using simple shift method...");

        StringBuilder sb = new StringBuilder();

        for (char c : password.toCharArray()) {
            // Shift digits by 3 (wrap around 0–9)
            if (Character.isDigit(c)) {
                sb.append((char) ('0' + (c - '0' + 3) % 10));
            }
            // Shift letters by 3 (wrap around a–z or A–Z)
            else if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                sb.append((char) (base + (c - base + 3) % 26));
            }
            // Leave symbols or others unchanged
            else {
                sb.append(c);
            }
        }

        System.out.println("Password encrypted successfully.");
        return sb.toString();
    }
}
