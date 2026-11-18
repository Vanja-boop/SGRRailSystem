package service;

import model.Transaction;

import java.time.LocalDateTime;

/** SIMULATED M-Pesa STK Push (a real one needs Safaricom's Daraja API). */
public class MpesaPayment extends PaymentMethod {
    private final String phone;

    public MpesaPayment(String phone) {
        if (!isValidPhone(phone)) throw new IllegalArgumentException("Invalid M-Pesa phone number.");
        this.phone = phone;
    }

    public static boolean isValidPhone(String phone) {
        return phone.matches("(\\+254|254|0)[17]\\d{8}");
    }

    public static boolean isValidPin(String pin) {
        return pin.matches("\\d{4}");
    }

    public String getPhone() { return phone; }

    @Override
    public String getName() { return "M-PESA"; }

    @Override
    public Transaction process(double amount) {
        return new Transaction(0, 0, generateReference("S"), amount, LocalDateTime.now(),
                "SUCCESS", getName(), null);
    }
}
