package model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Abstract parent of Passenger and Admin (authentication + roles). */
public abstract class User {
    private int userId;
    private String fullName;
    private String idNumber;
    private String phone;
    private String username;
    private String passwordHash;

    public User(int userId, String fullName, String idNumber, String phone,
                String username, String passwordHash) {
        this.userId = userId;
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.phone = phone;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public String getIdNumber() { return idNumber; }
    public String getPhone() { return phone; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }

    /** Each subclass reports its own role (polymorphism). */
    public abstract String getRole();

    /** Passwords are never stored as plain text. */
    public static String hashPassword(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | ID %s | %s | %s", userId, fullName, idNumber, phone, getRole());
    }
}
