package model;

import db.DatabaseManager;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Booking implements Payable {
    private int bookingId;
    private int passengerId;
    private int scheduleId;
    private String originStation;
    private String destinationStation;
    private String coachNumber;
    private String seatNumber;
    private LocalDate travelDate;
    private double fareAmount;
    private String bookingStatus;       // PENDING / CONFIRMED / CANCELLED

    private String passengerName;       // filled in when loaded from the database
    private Transaction payment;        // set after M-Pesa payment succeeds

    public Booking(int bookingId, int passengerId, int scheduleId, String originStation,
                   String destinationStation, String coachNumber, String seatNumber,
                   LocalDate travelDate, double fareAmount, String bookingStatus) {
        this.bookingId = bookingId;
        this.passengerId = passengerId;
        this.scheduleId = scheduleId;
        this.originStation = originStation;
        this.destinationStation = destinationStation;
        this.coachNumber = coachNumber;
        this.seatNumber = seatNumber;
        this.travelDate = travelDate;
        this.fareAmount = fareAmount;
        this.bookingStatus = bookingStatus;
    }

    /** Constructor for a brand-new booking that has not been confirmed yet. */
    public Booking(int passengerId, int scheduleId, String originStation,
                   String destinationStation, LocalDate travelDate, double fareAmount) {
        this(0, passengerId, scheduleId, originStation, destinationStation, null, null,
                travelDate, fareAmount, "PENDING");
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public int getPassengerId() { return passengerId; }
    public int getScheduleId() { return scheduleId; }
    public String getOriginStation() { return originStation; }
    public String getDestinationStation() { return destinationStation; }
    public String getCoachNumber() { return coachNumber; }
    public String getSeatNumber() { return seatNumber; }
    public LocalDate getTravelDate() { return travelDate; }
    public double getFareAmount() { return fareAmount; }
    public String getBookingStatus() { return bookingStatus; }
    public void setPassengerName(String name) { this.passengerName = name; }
    public String getPassengerName() { return passengerName; }
    public Transaction getPayment() { return payment; }
    public void setPayment(Transaction payment) { this.payment = payment; }

    @Override
    public double getAmount() { return fareAmount; }

    public SeatClass getSeatClass() {
        return SeatClass.forCoach(Integer.parseInt(coachNumber));
    }

    public void selectCoachAndSeat(String coach, String seat) {
        this.coachNumber = coach;
        this.seatNumber = seat;
    }

    /** Saves the booking (and its payment) if the seat is still free. */
    public boolean confirmBooking() {
        DatabaseManager db = DatabaseManager.getInstance();
        if (coachNumber == null || seatNumber == null || payment == null) return false;
        if (db.isSeatTaken(scheduleId, travelDate, coachNumber, seatNumber)) return false;
        this.bookingStatus = "CONFIRMED";
        db.saveRecord(this);
        return true;
    }

    public PrintableReceipt generateReceipt() {
        DatabaseManager db = DatabaseManager.getInstance();
        Transaction tx = db.findTransactionByBooking(bookingId);
        if (tx == null) throw new IllegalStateException("No payment found for booking #" + bookingId);
        Schedule s = (Schedule) db.findRecord("schedule", scheduleId);
        String dep = s == null ? "" : " dep " + s.getDepartureTime();
        String travel = String.format("%s -> %s, %s%s, Coach %s Seat %s (%s)", originStation,
                destinationStation, travelDate, dep, coachNumber, seatNumber, getSeatClass().getName());
        String number = String.format("RCT-%s-%05d", travelDate.toString().replace("-", ""), bookingId);
        LocalDateTime paid = tx.getPaymentDate();
        return new PrintableReceipt(number, tx.getAmount(), passengerName, paid, travel,
                tx.describeMethod(), tx.getReferenceCode());
    }

    public boolean cancelTicket() {
        if (!"CONFIRMED".equals(bookingStatus)) return false;
        this.bookingStatus = "CANCELLED";
        DatabaseManager.getInstance().updateRecord(this);
        return true;
    }

    @Override
    public String toString() {
        return String.format("Booking #%d | %s | %s -> %s | %s | Coach %s Seat %s | KES %.0f | %s",
                bookingId, passengerName == null ? "passenger #" + passengerId : passengerName,
                originStation, destinationStation, travelDate, coachNumber, seatNumber,
                fareAmount, bookingStatus);
    }
}
