package model;

public class Passenger extends User {
    public Passenger(int userId, String fullName, String idNumber, String phone,
                     String username, String passwordHash) {
        super(userId, fullName, idNumber, phone, username, passwordHash);
    }

    @Override
    public String getRole() { return "PASSENGER"; }
}
