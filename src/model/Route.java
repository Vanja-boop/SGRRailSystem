package model;

public class Route {
    private static final double RATE_PER_KM = 2.1;   // economy rate in KES per km (adjustable)

    private int routeId;
    private String origin;
    private String destination;
    private double distanceKm;

    public Route(int routeId, String origin, String destination, double distanceKm) {
        this.routeId = routeId;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
    }

    public int getRouteId() { return routeId; }
    public void setRouteId(int routeId) { this.routeId = routeId; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public double getDistanceKm() { return distanceKm; }

    public String getDetails() {
        return String.format("[%d] %s -> %s (%.0f km)", routeId, origin, destination, distanceKm);
    }

    /** Fare = distance x rate x class multiplier, rounded to the nearest KES 10. */
    public double calculateFare(SeatClass seatClass) {
        double raw = distanceKm * RATE_PER_KM * seatClass.getFareMultiplier();
        return Math.round(raw / 10.0) * 10.0;
    }

    @Override
    public String toString() { return getDetails(); }
}
