package service;

import model.Transaction;

import java.security.SecureRandom;

/** Abstract payment method. Each subclass processes a payment in its own way (polymorphism). */
public abstract class PaymentMethod {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public abstract String getName();

    /** Charges the amount and returns a SUCCESS transaction (booking id is filled in when saved). */
    public abstract Transaction process(double amount);

    protected static String generateReference(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 9; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }
}
