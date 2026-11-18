package model;

import java.time.LocalDateTime;

/** A payment attached to one booking (M-Pesa or card). */
public class Transaction implements Payable {
    private int transactionId;
    private int bookingId;
    private String referenceCode;       // M-Pesa code or card authorisation code
    private double amountPaid;
    private LocalDateTime paymentDate;
    private String status;              // SUCCESS / REFUND_PENDING
    private String paymentMethod;       // M-PESA / CARD
    private String cardLast4;           // only for card payments; never the full number

    public Transaction(int transactionId, int bookingId, String referenceCode, double amountPaid,
                       LocalDateTime paymentDate, String status, String paymentMethod, String cardLast4) {
        this.transactionId = transactionId;
        this.bookingId = bookingId;
        this.referenceCode = referenceCode;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.cardLast4 = cardLast4;
    }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int id) { this.transactionId = id; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public String getReferenceCode() { return referenceCode; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getCardLast4() { return cardLast4; }

    /** e.g. "M-PESA" or "CARD ending 4242". */
    public String describeMethod() {
        return cardLast4 == null ? paymentMethod : paymentMethod + " ending " + cardLast4;
    }

    @Override
    public double getAmount() { return amountPaid; }

    @Override
    public String toString() {
        return String.format("Txn #%d | booking #%d | %s | ref %s | KES %.0f | %s | %s",
                transactionId, bookingId, describeMethod(), referenceCode, amountPaid,
                paymentDate.toString().replace('T', ' ').substring(0, 16), status);
    }
}
