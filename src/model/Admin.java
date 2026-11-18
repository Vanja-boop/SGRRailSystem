package model;

import db.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

/** Admin extends User: inherits every attribute and adds management operations. */
public class Admin extends User {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public Admin(int userId, String fullName, String idNumber, String phone,
                 String username, String passwordHash) {
        super(userId, fullName, idNumber, phone, username, passwordHash);
    }

    @Override
    public String getRole() { return "ADMIN"; }

    // ----- Trains -----
    public void addTrain(Train t) { db.saveRecord(t); }
    public void updateTrain(Train t) { db.updateRecord(t); }
    public void deleteTrain(int id) { db.deleteRecord("train", id); }

    // ----- Routes -----
    public void addRoute(Route r) { db.saveRecord(r); }
    public void updateRoute(Route r) { db.updateRecord(r); }
    public void deleteRoute(int id) { db.deleteRecord("route", id); }

    // ----- Schedules -----
    public void addSchedule(Schedule s) { db.saveRecord(s); }
    public void updateSchedule(Schedule s) { db.updateRecord(s); }
    public void deleteSchedule(int id) { db.deleteRecord("schedule", id); }

    // ----- Auditing -----
    public List<Transaction> viewAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        for (Object o : db.getRecords("transaction")) list.add((Transaction) o);
        return list;
    }
}
