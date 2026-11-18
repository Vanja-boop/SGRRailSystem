package model;

public class FirstClass extends SeatClass {
    @Override public String getName() { return "First Class"; }
    @Override public double getFareMultiplier() { return 3.0; }
}
