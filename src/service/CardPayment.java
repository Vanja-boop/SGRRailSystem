package service;

import model.Transaction;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * SIMULATED card payment (Visa / Mastercard debit or credit).
 * The card is validated, then only the LAST 4 DIGITS are kept. The full number and the CVV
 * are never stored anywhere. A real system must use a payment gateway (e.g. Pesapal,
 * Flutterwave, DPO or Stripe) so that card data never touches your own code or database.
 */
public class CardPayment extends PaymentMethod {
    private final String holderName;
    private final String last4;

    /** Validates everything up front; throws IllegalArgumentException with a clear message. */
    public CardPayment(String holderName, String cardNumber, String expiry, String cvv) {
        String digits = cardNumber.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{13,19}")) throw new IllegalArgumentException("Card number must be 13 to 19 digits.");
        if (!passesLuhn(digits)) throw new IllegalArgumentException("Card number is not valid.");
        checkExpiry(expiry);
        if (!cvv.matches("\\d{3,4}")) throw new IllegalArgumentException("CVV must be 3 or 4 digits.");
        if (holderName.isBlank()) throw new IllegalArgumentException("Card holder name is required.");

        this.holderName = holderName;
        this.last4 = digits.substring(digits.length() - 4);   // the only part we keep
    }

    /** Luhn checksum: catches mistyped card numbers. */
    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    /** Expiry in MM/YY format; the card is valid until the end of that month. */
    private static void checkExpiry(String expiry) {
        if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new IllegalArgumentException("Expiry must be in the format MM/YY, e.g. 08/28.");
        }
        try {
            int month = Integer.parseInt(expiry.substring(0, 2));
            int year = 2000 + Integer.parseInt(expiry.substring(3));
            if (YearMonth.of(year, month).isBefore(YearMonth.now())) {
                throw new IllegalArgumentException("This card has expired.");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid expiry date.");
        }
    }

    public String getHolderName() { return holderName; }
    public String getLast4() { return last4; }

    @Override
    public String getName() { return "CARD"; }

    @Override
    public Transaction process(double amount) {
        return new Transaction(0, 0, generateReference("C"), amount, LocalDateTime.now(),
                "SUCCESS", getName(), last4);
    }
}
