import db.DatabaseException;
import db.DatabaseManager;
import model.Admin;
import model.Passenger;
import model.User;
import ui.AdminMenu;
import ui.Input;
import ui.PassengerMenu;

public class Main {
    public static void main(String[] args) {
        DatabaseManager db = DatabaseManager.getInstance();
        try {
            db.connect();
        } catch (DatabaseException e) {
            System.out.println(e.getMessage());
            System.out.println("Check that the sqlite-jdbc jar is inside the lib folder.");
            return;
        }

        System.out.println("=== SGR RAIL MANAGEMENT SYSTEM (Madaraka Express) ===");
        boolean running = true;
        while (running) {
            System.out.println("\n1. Login   2. Register as passenger   0. Exit");
            int choice = Input.integer("Choose: ");
            try {
                switch (choice) {
                    case 1 -> login(db);
                    case 2 -> register(db);
                    case 0 -> running = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        db.disconnect();
        System.out.println("Goodbye!");
    }

    private static void login(DatabaseManager db) {
        String username = Input.text("Username: ");
        String password = Input.text("Password: ");
        User user = db.authenticate(username, password);
        if (user == null) {
            System.out.println("Wrong username or password.");
        } else if (user instanceof Admin admin) {
            AdminMenu.run(admin);
        } else if (user instanceof Passenger passenger) {
            PassengerMenu.run(passenger);
        }
    }

    private static void register(DatabaseManager db) {
        String name = Input.text("Full name: ");
        String idNo = Input.text("National ID / Passport number: ");
        String phone = Input.text("Phone number: ");
        String username = Input.text("Choose a username: ");
        String password = Input.text("Choose a password (min 6 characters): ");
        if (password.length() < 6) {
            System.out.println("Password too short.");
            return;
        }
        db.saveRecord(new Passenger(0, name, idNo, phone, username, User.hashPassword(password)));
        System.out.println("Registered! You can now log in.");
    }
}
