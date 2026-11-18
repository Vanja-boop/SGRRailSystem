package model;

public class EconomyClass extends SeatClass {
    @Override public String getName() { return "Economy"; }
    @Override public double getFareMultiplier() { return 1.0; }
}
