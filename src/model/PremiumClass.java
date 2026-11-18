package model;

public class PremiumClass extends SeatClass {
    @Override public String getName() { return "Premium"; }
    @Override public double getFareMultiplier() { return 2.0; }
}
