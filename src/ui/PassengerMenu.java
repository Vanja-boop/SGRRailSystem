package ui;

import db.DatabaseManager;
import model.Booking;
import model.EconomyClass;
import model.FirstClass;
import model.Passenger;
import model.PremiumClass;
import model.PrintableReceipt;
import model.Route;
import model.Schedule;
import model.SeatClass;
import model.Train;
import model.Transaction;
import service.CardPayment;
import service.MpesaPayment;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class PassengerMenu {
    private static final DatabaseManager db = DatabaseManager.getInstance();

    public static void run(Passenger p) {
        System.out.println("\nWelcome, " + p.getFullName() + "!");
        boolean open = true;
        while (open) {
            System.out.println("\n--- PASSENGER MENU ---");
            System.out.println("1. View schedules   2. Book a ticket   3. My bookings");
            System.out.println("4. Cancel a ticket  5. Print a receipt 0. Logout");
            int choice = Input.integer("Choose: ");
            try {
                switch (choice) {
                    case 1 -> db.getRecords("schedule").forEach(o -> System.out.println(((Schedule) o).getDetails()));
                    case 2 -> bookTicket(p);
                    case 3 -> myBookings(p);
                    case 4 -> cancelTicket(p);
                    case 5 -> printReceipt(p);
                    case 0 -> open = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void bookTicket(Passenger p) {
        db.getRecords("schedule").forEach(o -> System.out.println(((Schedule) o).getDetails()));
        Schedule s = (Schedule) db.findRecord("schedule", Input.integer("Schedule id: "));
        if (s == null) { System.out.println("Schedule not found."); return; }

        Train train = (Train) db.findRecord("train", s.getTrainId());
        Route route = (Route) db.findRecord("route", s.getRouteId());
        if (!"Active".equalsIgnoreCase(train.getStatus())) {
            System.out.println("That train is currently " + train.getStatus() + ". Please choose another schedule.");
            return;
        }

        LocalDate date = Input.date("Travel date (YYYY-MM-DD): ");
        System.out.println("\nFares for " + route.getOrigin() + " -> " + route.getDestination() + ":");
        System.out.printf("  Coach 1 = First Class KES %.0f | Coach 2 = Premium KES %.0f | Coaches 3-%d = Economy KES %.0f%n",
                route.calculateFare(new FirstClass()), route.calculateFare(new PremiumClass()),
                train.getTotalCoaches(), route.calculateFare(new EconomyClass()));

        int coach = Input.integer("Coach number (1-" + train.getTotalCoaches() + "): ");
        if (coach < 1 || coach > train.getTotalCoaches()) { System.out.println("No such coach."); return; }
        List<String> taken = db.getTakenSeats(s.getScheduleId(), date, String.valueOf(coach));
        System.out.println("Seats already taken in coach " + coach + ": " + (taken.isEmpty() ? "none" : taken));
        int seat = Input.integer("Seat number (1-" + train.getSeatsPerCoach() + "): ");
        if (seat < 1 || seat > train.getSeatsPerCoach()) { System.out.println("No such seat."); return; }

        SeatClass seatClass = SeatClass.forCoach(coach);
        double fare = route.calculateFare(seatClass);          // polymorphism: each class prices itself
        Booking b = new Booking(p.getUserId(), s.getScheduleId(), route.getOrigin(), route.getDestination(), date, fare);
        b.setPassengerName(p.getFullName());
        b.selectCoachAndSeat(String.valueOf(coach), String.valueOf(seat));

        System.out.printf("%nSummary: %s -> %s on %s, Coach %d Seat %d (%s), fare KES %.0f%n",
                route.getOrigin(), route.getDestination(), date, coach, seat, seatClass.getName(), fare);
        if (!Input.confirm("Proceed to payment?")) { System.out.println("Booking cancelled."); return; }

        System.out.println("Pay with:  1. M-Pesa   2. Debit / Credit card (Visa, Mastercard)");
        int method = Input.integer("Choose: ");
        Transaction tx;
        if (method == 1) tx = payWithMpesa(fare);
        else if (method == 2) tx = payWithCard(fare);
        else { System.out.println("Invalid payment option."); return; }
        if (tx == null) return;   // payment failed or was cancelled; nothing was booked

        b.setPayment(tx);
        if (b.confirmBooking()) {
            System.out.println("\nPayment received via " + tx.describeMethod() + " (" + tx.getReferenceCode() + "). Booking confirmed!\n");
            PrintableReceipt r = b.generateReceipt();
            System.out.println(r.print());
            Path saved = r.saveToFile();
            System.out.println("Receipt saved to: " + saved);
        } else {
            System.out.println("Sorry, that seat was just taken. You have not been booked; please try again.");
        }
    }

    /** Returns a successful Transaction, or null if the payment failed. */
    private static Transaction payWithMpesa(double fare) {
        String phone = Input.text("M-Pesa phone number (e.g. 0712345678): ");
        if (!MpesaPayment.isValidPhone(phone)) { System.out.println("Invalid phone number."); return null; }
        System.out.println("STK Push sent to " + phone + " (simulation).");
        String pin = Input.text("Enter M-Pesa PIN to approve (any 4 digits in this demo): ");
        if (!MpesaPayment.isValidPin(pin)) { System.out.println("Payment failed: PIN must be 4 digits."); return null; }
        return new MpesaPayment(phone).process(fare);
    }

    private static Transaction payWithCard(double fare) {
        System.out.println("Card payment (simulation). Your full card number and CVV are NOT stored.");
        String holder = Input.text("Name on card: ");
        String number = Input.text("Card number: ");
        String expiry = Input.text("Expiry date (MM/YY): ");
        String cvv = Input.text("CVV (3 or 4 digits on the back): ");
        try {
            CardPayment card = new CardPayment(holder, number, expiry, cvv);
            System.out.println("Contacting your bank... approved.");
            return card.process(fare);
        } catch (IllegalArgumentException e) {
            System.out.println("Card declined: " + e.getMessage());
            return null;
        }
    }

    private static void myBookings(Passenger p) {
        List<Booking> list = db.getBookingsForPassenger(p.getUserId());
        if (list.isEmpty()) System.out.println("You have no bookings yet.");
        list.forEach(System.out::println);
    }

    private static Booking ownBooking(Passenger p, int id) {
        Object o = db.findRecord("booking", id);
        if (o == null || ((Booking) o).getPassengerId() != p.getUserId()) return null;
        return (Booking) o;
    }

    private static void cancelTicket(Passenger p) {
        myBookings(p);
        Booking b = ownBooking(p, Input.integer("Booking id to cancel: "));
        if (b == null) { System.out.println("Booking not found."); return; }
        System.out.println(b.cancelTicket()
                ? "Ticket cancelled. Your refund is marked pending."
                : "Only CONFIRMED tickets can be cancelled.");
    }

    private static void printReceipt(Passenger p) {
        myBookings(p);
        Booking b = ownBooking(p, Input.integer("Booking id: "));
        if (b == null) { System.out.println("Booking not found."); return; }
        System.out.println(b.generateReceipt().print());
    }
}
