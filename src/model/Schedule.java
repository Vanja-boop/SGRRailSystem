package model;

import db.DatabaseManager;

import java.time.Duration;
import java.time.LocalTime;

public class Schedule {
    private int scheduleId;
    private int routeId;
    private int trainId;
    private LocalTime departureTime;
    private LocalTime arrivalTime;

    public Schedule(int scheduleId, int routeId, int trainId, LocalTime departureTime, LocalTime arrivalTime) {
        this.scheduleId = scheduleId;
        this.routeId = routeId;
        this.trainId = trainId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }
    public int getRouteId() { return routeId; }
    public int getTrainId() { return trainId; }
    public LocalTime getDepartureTime() { return departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }

    /** Journey time in minutes (handles arrival after midnight). */
    public int getDuration() {
        long mins = Duration.between(departureTime, arrivalTime).toMinutes();
        if (mins < 0) mins += 24 * 60;
        return (int) mins;
    }

    public String getDetails() {
        DatabaseManager db = DatabaseManager.getInstance();
        Route r = (Route) db.findRecord("route", routeId);
        Train t = (Train) db.findRecord("train", trainId);
        String route = r == null ? "route #" + routeId : r.getOrigin() + " -> " + r.getDestination();
        String train = t == null ? "train #" + trainId : t.getTrainName() + " (" + t.getStatus() + ")";
        return String.format("[%d] %s | %s | dep %s arr %s (%dh %02dm)", scheduleId, route, train,
                departureTime, arrivalTime, getDuration() / 60, getDuration() % 60);
    }

    @Override
    public String toString() { return getDetails(); }
}
