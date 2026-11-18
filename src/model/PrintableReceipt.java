package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class PrintableReceipt {
    private String receiptNumber;
    private double amountPaid;
    private String passengerName;
    private LocalDateTime paymentDate;
    private String travelDetails;
    private String paymentMethod;
    private String referenceCode;
    private String qrCodeData;

    public PrintableReceipt(String receiptNumber, double amountPaid, String passengerName,
                            LocalDateTime paymentDate, String travelDetails,
                            String paymentMethod, String referenceCode) {
        this.receiptNumber = receiptNumber;
        this.amountPaid = amountPaid;
        this.passengerName = passengerName;
        this.paymentDate = paymentDate;
        this.travelDetails = travelDetails;
        this.paymentMethod = paymentMethod;
        this.referenceCode = referenceCode;
        // The text a QR code would carry; scanners at the gate would read this.
        this.qrCodeData = "SGR|" + receiptNumber + "|" + passengerName + "|" + referenceCode;
    }

    public String getReceiptNumber() { return receiptNumber; }
    public String getQrCodeData() { return qrCodeData; }

    public String print() {
        String line = "=".repeat(52);
        return line + "\n"
             + "      KENYA RAILWAYS - MADARAKA EXPRESS (SGR)\n"
             + "                 TICKET RECEIPT\n"
             + line + "\n"
             + "Receipt no : " + receiptNumber + "\n"
             + "Passenger  : " + passengerName + "\n"
             + "Journey    : " + travelDetails + "\n"
             + "Paid via   : " + paymentMethod + "\n"
             + "Reference  : " + referenceCode + "\n"
             + String.format("Amount paid: KES %.0f%n", amountPaid)
             + "Paid on    : " + paymentDate.toString().replace('T', ' ').substring(0, 16) + "\n"
             + "Boarding code: " + qrCodeData + "\n"
             + line;
    }

    /** Saves the receipt as a text file in the "receipts" folder. */
    public Path saveToFile() {
        try {
            Path dir = Path.of("receipts");
            Files.createDirectories(dir);
            return Files.writeString(dir.resolve(receiptNumber + ".txt"), print());
        } catch (IOException e) {
            throw new RuntimeException("Could not save receipt: " + e.getMessage(), e);
        }
    }
}
