package model;

/** Abstract class: each carriage class prices itself differently (polymorphism). */
public abstract class SeatClass {
    public abstract String getName();
    public abstract double getFareMultiplier();

    /** Coach 1 = First Class, Coach 2 = Premium, all others = Economy. */
    public static SeatClass forCoach(int coachNumber) {
        if (coachNumber == 1) return new FirstClass();
        if (coachNumber == 2) return new PremiumClass();
        return new EconomyClass();
    }
}
