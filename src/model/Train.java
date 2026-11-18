package model;

import db.DatabaseManager;

public class Train {
    // Adjust these if your lecturer gives real capacities.
    private static final int EXPRESS_SEATS_PER_COACH = 60;
    private static final int INTERCOUNTY_SEATS_PER_COACH = 80;

    private int trainId;
    private String trainName;
    private String trainType;   // Express / Intercounty
    private String status;      // Active / Maintenance
    private int totalCoaches;

    public Train(int trainId, String trainName, String trainType, String status, int totalCoaches) {
        this.trainId = trainId;
        this.trainName = trainName;
        this.trainType = trainType;
        this.status = status;
        this.totalCoaches = totalCoaches;
    }

    public int getTrainId() { return trainId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }
    public String getTrainName() { return trainName; }
    public String getTrainType() { return trainType; }
    public String getStatus() { return status; }
    public int getTotalCoaches() { return totalCoaches; }

    public int getSeatsPerCoach() {
        return "Express".equalsIgnoreCase(trainType) ? EXPRESS_SEATS_PER_COACH : INTERCOUNTY_SEATS_PER_COACH;
    }

    public String getDetails() {
        return String.format("[%d] %s | %s | %s | %d coaches", trainId, trainName, trainType, status, totalCoaches);
    }

    /** Updates the status and saves it to the database. */
    public void updateStatus(String status) {
        this.status = status;
        DatabaseManager.getInstance().updateRecord(this);
    }

    /** Total seats across all coaches. */
    public int getCoachCapacity() {
        return totalCoaches * getSeatsPerCoach();
    }

    @Override
    public String toString() { return getDetails(); }
}
