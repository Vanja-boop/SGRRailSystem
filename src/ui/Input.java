package ui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Safe console input helpers (re-ask instead of crashing on bad input). */
public final class Input {
    private static final Scanner IN = new Scanner(System.in);

    private Input() { }

    public static String text(String prompt) {
        String s;
        do {
            System.out.print(prompt);
            s = IN.nextLine().trim();
        } while (s.isEmpty());
        return s;
    }

    /** May return an empty string (used for "press Enter to keep the current value"). */
    public static String optional(String prompt) {
        System.out.print(prompt);
        return IN.nextLine().trim();
    }

    public static int integer(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(IN.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    public static double number(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(IN.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    public static LocalDate date(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                LocalDate d = LocalDate.parse(IN.nextLine().trim());
                if (d.isBefore(LocalDate.now())) {
                    System.out.println("The date cannot be in the past.");
                    continue;
                }
                return d;
            } catch (DateTimeParseException e) {
                System.out.println("Use the format YYYY-MM-DD, e.g. 2026-12-20.");
            }
        }
    }

    public static LocalTime time(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return LocalTime.parse(IN.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("Use the 24-hour format HH:MM, e.g. 08:00.");
            }
        }
    }

    public static boolean confirm(String prompt) {
        System.out.print(prompt + " (y/n): ");
        return IN.nextLine().trim().equalsIgnoreCase("y");
    }
}
