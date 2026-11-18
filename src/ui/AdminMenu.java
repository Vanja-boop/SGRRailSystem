package ui;

import db.DatabaseManager;
import model.Admin;
import model.Route;
import model.Schedule;
import model.Train;

import java.time.LocalTime;

public class AdminMenu {
    private static final DatabaseManager db = DatabaseManager.getInstance();

    public static void run(Admin admin) {
        System.out.println("\nWelcome, " + admin.getFullName() + " (Admin).");
        boolean open = true;
        while (open) {
            System.out.println("\n--- ADMIN MENU ---");
            System.out.println("1. Manage trains     2. Manage routes     3. Manage schedules");
            System.out.println("4. All transactions  5. All bookings      0. Logout");
            int choice = Input.integer("Choose: ");
            try {
                switch (choice) {
                    case 1 -> trains(admin);
                    case 2 -> routes(admin);
                    case 3 -> schedules(admin);
                    case 4 -> {
                        var list = admin.viewAllTransactions();
                        if (list.isEmpty()) System.out.println("No transactions yet.");
                        list.forEach(System.out::println);
                        System.out.printf("Total collected: KES %.0f%n",
                                list.stream().filter(t -> "SUCCESS".equals(t.getStatus())).mapToDouble(t -> t.getAmount()).sum());
                    }
                    case 5 -> db.getRecords("booking").forEach(System.out::println);
                    case 0 -> open = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static int subMenu(String name) {
        System.out.println("\n[" + name + "] 1. List  2. Add  3. Update  4. Delete  0. Back");
        return Input.integer("Choose: ");
    }

    // ---------------- trains ----------------
    private static void trains(Admin admin) {
        int c;
        while ((c = subMenu("TRAINS")) != 0) {
            try {
                switch (c) {
                    case 1 -> db.getRecords("train").forEach(System.out::println);
                    case 2 -> admin.addTrain(new Train(0, Input.text("Train name: "), askType(),
                            askStatus(), Input.integer("Total coaches: ")));
                    case 3 -> {
                        db.getRecords("train").forEach(System.out::println);
                        Train t = (Train) db.findRecord("train", Input.integer("Train id to update: "));
                        if (t == null) { System.out.println("Train not found."); break; }
                        String name = Input.optional("New name (Enter to keep '" + t.getTrainName() + "'): ");
                        System.out.println("New status:");
                        String status = askStatus();
                        int coaches = Input.integer("Total coaches (currently " + t.getTotalCoaches() + "): ");
                        admin.updateTrain(new Train(t.getTrainId(), name.isEmpty() ? t.getTrainName() : name,
                                t.getTrainType(), status, coaches));
                        System.out.println("Train updated.");
                    }
                    case 4 -> {
                        db.getRecords("train").forEach(System.out::println);
                        admin.deleteTrain(Input.integer("Train id to delete: "));
                        System.out.println("Done.");
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static String askType() {
        return Input.integer("Type (1 = Express, 2 = Intercounty): ") == 2 ? "Intercounty" : "Express";
    }

    private static String askStatus() {
        return Input.integer("Status (1 = Active, 2 = Maintenance): ") == 2 ? "Maintenance" : "Active";
    }

    // ---------------- routes ----------------
    private static void routes(Admin admin) {
        int c;
        while ((c = subMenu("ROUTES")) != 0) {
            try {
                switch (c) {
                    case 1 -> db.getRecords("route").forEach(System.out::println);
                    case 2 -> admin.addRoute(new Route(0, Input.text("Origin: "), Input.text("Destination: "),
                            Input.number("Distance (km): ")));
                    case 3 -> {
                        db.getRecords("route").forEach(System.out::println);
                        Route r = (Route) db.findRecord("route", Input.integer("Route id to update: "));
                        if (r == null) { System.out.println("Route not found."); break; }
                        admin.updateRoute(new Route(r.getRouteId(), r.getOrigin(), r.getDestination(),
                                Input.number("New distance in km (currently " + r.getDistanceKm() + "): ")));
                        System.out.println("Route updated.");
                    }
                    case 4 -> {
                        db.getRecords("route").forEach(System.out::println);
                        admin.deleteRoute(Input.integer("Route id to delete: "));
                        System.out.println("Done.");
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ---------------- schedules ----------------
    private static void schedules(Admin admin) {
        int c;
        while ((c = subMenu("SCHEDULES")) != 0) {
            try {
                switch (c) {
                    case 1 -> db.getRecords("schedule").forEach(o -> System.out.println(((Schedule) o).getDetails()));
                    case 2 -> {
                        db.getRecords("route").forEach(System.out::println);
                        int routeId = Input.integer("Route id: ");
                        db.getRecords("train").forEach(System.out::println);
                        int trainId = Input.integer("Train id: ");
                        LocalTime dep = Input.time("Departure time (HH:MM): ");
                        LocalTime arr = Input.time("Arrival time (HH:MM): ");
                        admin.addSchedule(new Schedule(0, routeId, trainId, dep, arr));
                        System.out.println("Schedule added.");
                    }
                    case 3 -> {
                        db.getRecords("schedule").forEach(o -> System.out.println(((Schedule) o).getDetails()));
                        Schedule s = (Schedule) db.findRecord("schedule", Input.integer("Schedule id to update: "));
                        if (s == null) { System.out.println("Schedule not found."); break; }
                        LocalTime dep = Input.time("New departure time (HH:MM): ");
                        LocalTime arr = Input.time("New arrival time (HH:MM): ");
                        admin.updateSchedule(new Schedule(s.getScheduleId(), s.getRouteId(), s.getTrainId(), dep, arr));
                        System.out.println("Schedule updated.");
                    }
                    case 4 -> {
                        db.getRecords("schedule").forEach(o -> System.out.println(((Schedule) o).getDetails()));
                        admin.deleteSchedule(Input.integer("Schedule id to delete: "));
                        System.out.println("Done.");
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}
