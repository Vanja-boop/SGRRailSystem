package db;

import model.Admin;
import model.Booking;
import model.Passenger;
import model.Route;
import model.Schedule;
import model.Train;
import model.Transaction;
import model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton: one shared, thread-safe connection for the whole program.
 * saveRecord / updateRecord / deleteRecord / getRecords work on any entity (polymorphic).
 */
public class DatabaseManager {
    private static DatabaseManager instance;

    private Connection connection;
    private final String databaseUrl = "jdbc:sqlite:sgr.db";

    private DatabaseManager() { }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // ================= connection =================
    public synchronized void connect() {
        try {
            if (connection != null && !connection.isClosed()) return;
            connection = DriverManager.getConnection(databaseUrl);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            createTables();
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to the database: " + e.getMessage(), e);
        }
    }

    public synchronized void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            throw new DatabaseException("Could not close the database: " + e.getMessage(), e);
        }
    }

    private Connection conn() {
        try {
            if (connection == null || connection.isClosed()) connect();
        } catch (SQLException e) {
            throw wrap(e);
        }
        return connection;
    }

    // ================= schema + sample data =================
    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT NOT NULL, "
                    + "id_number TEXT NOT NULL UNIQUE, phone TEXT NOT NULL, "
                    + "username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, role TEXT NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS trains ("
                    + "train_id INTEGER PRIMARY KEY AUTOINCREMENT, train_name TEXT NOT NULL, "
                    + "train_type TEXT NOT NULL, status TEXT NOT NULL, total_coaches INTEGER NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS routes ("
                    + "route_id INTEGER PRIMARY KEY AUTOINCREMENT, origin TEXT NOT NULL, "
                    + "destination TEXT NOT NULL, distance_km REAL NOT NULL, UNIQUE(origin, destination))");
            st.execute("CREATE TABLE IF NOT EXISTS schedules ("
                    + "schedule_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "route_id INTEGER NOT NULL REFERENCES routes(route_id), "
                    + "train_id INTEGER NOT NULL REFERENCES trains(train_id), "
                    + "departure_time TEXT NOT NULL, arrival_time TEXT NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS bookings ("
                    + "booking_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "passenger_id INTEGER NOT NULL REFERENCES users(user_id), "
                    + "schedule_id INTEGER NOT NULL REFERENCES schedules(schedule_id), "
                    + "origin_station TEXT NOT NULL, destination_station TEXT NOT NULL, "
                    + "coach_number TEXT NOT NULL, seat_number TEXT NOT NULL, travel_date TEXT NOT NULL, "
                    + "fare_amount REAL NOT NULL, booking_status TEXT NOT NULL)");
            // Safety net: the same seat cannot be CONFIRMED twice for one trip.
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_seat ON bookings"
                    + "(schedule_id, travel_date, coach_number, seat_number) WHERE booking_status = 'CONFIRMED'");
            st.execute("CREATE TABLE IF NOT EXISTS transactions ("
                    + "transaction_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "booking_id INTEGER NOT NULL REFERENCES bookings(booking_id), "
                    + "mpesa_ref_code TEXT NOT NULL UNIQUE, amount_paid REAL NOT NULL, "
                    + "payment_date TEXT NOT NULL, status TEXT NOT NULL, "
                    + "payment_method TEXT NOT NULL DEFAULT 'M-PESA', card_last4 TEXT)");
            // Upgrade databases created before card payments existed (keeps old data).
            addColumnIfMissing("transactions", "payment_method", "TEXT NOT NULL DEFAULT 'M-PESA'");
            addColumnIfMissing("transactions", "card_last4", "TEXT");

            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) seed(st);
            }
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) throws SQLException {
        boolean found = false;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) found = true;
            }
        }
        if (!found) {
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }

    /** Sample data (distances and times are examples; change them to match your project brief). */
    private void seed(Statement st) throws SQLException {
        st.execute("INSERT INTO users(full_name,id_number,phone,username,password_hash,role) VALUES "
                + "('System Admin','00000000','0700000000','admin','"
                + User.hashPassword("admin123") + "','ADMIN')");
        st.execute("INSERT INTO trains(train_name,train_type,status,total_coaches) VALUES "
                + "('Madaraka Express 1','Express','Active',8),"
                + "('Madaraka Express 2','Express','Active',8),"
                + "('Inter-County 1','Intercounty','Active',6),"
                + "('Inter-County 2','Intercounty','Maintenance',6)");
        st.execute("INSERT INTO routes(origin,destination,distance_km) VALUES "
                + "('Nairobi','Mombasa',472),('Mombasa','Nairobi',472),"
                + "('Nairobi','Voi',325),('Nairobi','Emali',150)");
        st.execute("INSERT INTO schedules(route_id,train_id,departure_time,arrival_time) VALUES "
                + "(1,1,'08:00','13:00'),(2,2,'15:00','20:00'),(3,3,'09:00','12:30'),(4,3,'14:00','15:30')");
    }

    // ================= CRUD (polymorphic) =================
    public synchronized void saveRecord(Object o) {
        try {
            if (o instanceof User u) insertUser(u);
            else if (o instanceof Train t) insertTrain(t);
            else if (o instanceof Route r) insertRoute(r);
            else if (o instanceof Schedule s) insertSchedule(s);
            else if (o instanceof Booking b) insertBooking(b);
            else throw new IllegalArgumentException("Cannot save: " + o.getClass().getSimpleName());
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    public synchronized void updateRecord(Object o) {
        try {
            if (o instanceof Train t) {
                run("UPDATE trains SET train_name=?, train_type=?, status=?, total_coaches=? WHERE train_id=?",
                        t.getTrainName(), t.getTrainType(), t.getStatus(), t.getTotalCoaches(), t.getTrainId());
            } else if (o instanceof Route r) {
                run("UPDATE routes SET origin=?, destination=?, distance_km=? WHERE route_id=?",
                        r.getOrigin(), r.getDestination(), r.getDistanceKm(), r.getRouteId());
            } else if (o instanceof Schedule s) {
                run("UPDATE schedules SET route_id=?, train_id=?, departure_time=?, arrival_time=? WHERE schedule_id=?",
                        s.getRouteId(), s.getTrainId(), s.getDepartureTime().toString(),
                        s.getArrivalTime().toString(), s.getScheduleId());
            } else if (o instanceof Booking b) {
                run("UPDATE bookings SET booking_status=? WHERE booking_id=?", b.getBookingStatus(), b.getBookingId());
                if ("CANCELLED".equals(b.getBookingStatus())) {
                    run("UPDATE transactions SET status='REFUND_PENDING' WHERE booking_id=?", b.getBookingId());
                }
            } else {
                throw new IllegalArgumentException("Cannot update: " + o.getClass().getSimpleName());
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** The slide shows deleteRecord(int id); a table name is needed too so we know which record. */
    public synchronized void deleteRecord(String type, int id) {
        try {
            run("DELETE FROM " + tableOf(type) + " WHERE " + plainIdColumn(type) + " = ?", id);
        } catch (SQLException e) {
            throw wrap(e);
        }
    }

    /** type: train, route, schedule, passenger, booking, transaction. */
    public synchronized List<Object> getRecords(String type) {
        return query(type, "", new Object[0]);
    }

    public synchronized Object findRecord(String type, int id) {
        List<Object> list = query(type, " AND " + idColumn(type) + " = ?", new Object[] { id });
        return list.isEmpty() ? null : list.get(0);
    }

    // ================= extra helpers =================
    public synchronized User authenticate(String username, String plainPassword) {
        List<Object> list = query("user", " AND username = ? AND password_hash = ?",
                new Object[] { username, User.hashPassword(plainPassword) });
        return list.isEmpty() ? null : (User) list.get(0);
    }

    public synchronized List<Booking> getBookingsForPassenger(int passengerId) {
        List<Booking> result = new ArrayList<>();
        for (Object o : query("booking", " AND b.passenger_id = ?", new Object[] { passengerId })) {
            result.add((Booking) o);
        }
        return result;
    }

    public synchronized boolean isSeatTaken(int scheduleId, LocalDate date, String coach, String seat) {
        return getTakenSeats(scheduleId, date, coach).contains(seat);
    }

    public synchronized List<String> getTakenSeats(int scheduleId, LocalDate date, String coach) {
        List<String> seats = new ArrayList<>();
        String sql = "SELECT seat_number FROM bookings WHERE schedule_id=? AND travel_date=? "
                   + "AND coach_number=? AND booking_status='CONFIRMED' ORDER BY CAST(seat_number AS INTEGER)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, scheduleId, date.toString(), coach);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) seats.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
        return seats;
    }

    public synchronized Transaction findTransactionByBooking(int bookingId) {
        List<Object> list = query("transaction", " AND booking_id = ?", new Object[] { bookingId });
        return list.isEmpty() ? null : (Transaction) list.get(0);
    }

    // ================= inserts =================
    private void insertUser(User u) throws SQLException {
        u.setUserId(insert("INSERT INTO users(full_name,id_number,phone,username,password_hash,role) VALUES (?,?,?,?,?,?)",
                u.getFullName(), u.getIdNumber(), u.getPhone(), u.getUsername(), u.getPasswordHash(), u.getRole()));
    }

    private void insertTrain(Train t) throws SQLException {
        t.setTrainId(insert("INSERT INTO trains(train_name,train_type,status,total_coaches) VALUES (?,?,?,?)",
                t.getTrainName(), t.getTrainType(), t.getStatus(), t.getTotalCoaches()));
    }

    private void insertRoute(Route r) throws SQLException {
        r.setRouteId(insert("INSERT INTO routes(origin,destination,distance_km) VALUES (?,?,?)",
                r.getOrigin(), r.getDestination(), r.getDistanceKm()));
    }

    private void insertSchedule(Schedule s) throws SQLException {
        s.setScheduleId(insert("INSERT INTO schedules(route_id,train_id,departure_time,arrival_time) VALUES (?,?,?,?)",
                s.getRouteId(), s.getTrainId(), s.getDepartureTime().toString(), s.getArrivalTime().toString()));
    }

    /** Booking and its payment are saved together: both succeed or neither does. */
    private void insertBooking(Booking b) throws SQLException {
        Connection c = conn();
        c.setAutoCommit(false);
        try {
            b.setBookingId(insert("INSERT INTO bookings(passenger_id,schedule_id,origin_station,destination_station,"
                    + "coach_number,seat_number,travel_date,fare_amount,booking_status) VALUES (?,?,?,?,?,?,?,?,?)",
                    b.getPassengerId(), b.getScheduleId(), b.getOriginStation(), b.getDestinationStation(),
                    b.getCoachNumber(), b.getSeatNumber(), b.getTravelDate().toString(),
                    b.getFareAmount(), b.getBookingStatus()));
            Transaction t = b.getPayment();
            if (t != null) {
                t.setBookingId(b.getBookingId());
                t.setTransactionId(insert("INSERT INTO transactions(booking_id,mpesa_ref_code,amount_paid,payment_date,status,"
                        + "payment_method,card_last4) VALUES (?,?,?,?,?,?,?)", t.getBookingId(), t.getReferenceCode(),
                        t.getAmount(), t.getPaymentDate().toString(), t.getStatus(),
                        t.getPaymentMethod(), t.getCardLast4()));
            }
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    // ================= reading =================
    private List<Object> query(String type, String extraWhere, Object[] params) {
        String sql = baseSql(type) + extraWhere + " ORDER BY " + idColumn(type);
        List<Object> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(type, rs));
            }
        } catch (SQLException e) {
            throw wrap(e);
        }
        return list;
    }

    private String baseSql(String type) {
        return switch (type.toLowerCase()) {
            case "train" -> "SELECT * FROM trains WHERE 1=1";
            case "route" -> "SELECT * FROM routes WHERE 1=1";
            case "schedule" -> "SELECT * FROM schedules WHERE 1=1";
            case "user" -> "SELECT * FROM users WHERE 1=1";
            case "passenger" -> "SELECT * FROM users WHERE role = 'PASSENGER'";
            case "transaction" -> "SELECT * FROM transactions WHERE 1=1";
            case "booking" -> "SELECT b.*, u.full_name FROM bookings b "
                    + "JOIN users u ON b.passenger_id = u.user_id WHERE 1=1";
            default -> throw new IllegalArgumentException("Unknown record type: " + type);
        };
    }

    private String idColumn(String type) {
        return type.equalsIgnoreCase("booking") ? "b.booking_id" : plainIdColumn(type);
    }

    private String plainIdColumn(String type) {
        return switch (type.toLowerCase()) {
            case "train" -> "train_id";
            case "route" -> "route_id";
            case "schedule" -> "schedule_id";
            case "user", "passenger" -> "user_id";
            case "transaction" -> "transaction_id";
            case "booking" -> "booking_id";
            default -> throw new IllegalArgumentException("Unknown record type: " + type);
        };
    }

    private String tableOf(String type) {
        return switch (type.toLowerCase()) {
            case "train" -> "trains";
            case "route" -> "routes";
            case "schedule" -> "schedules";
            case "user", "passenger" -> "users";
            case "transaction" -> "transactions";
            case "booking" -> "bookings";
            default -> throw new IllegalArgumentException("Unknown record type: " + type);
        };
    }

    private Object map(String type, ResultSet rs) throws SQLException {
        switch (type.toLowerCase()) {
            case "train":
                return new Train(rs.getInt("train_id"), rs.getString("train_name"),
                        rs.getString("train_type"), rs.getString("status"), rs.getInt("total_coaches"));
            case "route":
                return new Route(rs.getInt("route_id"), rs.getString("origin"),
                        rs.getString("destination"), rs.getDouble("distance_km"));
            case "schedule":
                return new Schedule(rs.getInt("schedule_id"), rs.getInt("route_id"), rs.getInt("train_id"),
                        LocalTime.parse(rs.getString("departure_time")), LocalTime.parse(rs.getString("arrival_time")));
            case "user":
            case "passenger":
                if ("ADMIN".equals(rs.getString("role"))) {
                    return new Admin(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("id_number"),
                            rs.getString("phone"), rs.getString("username"), rs.getString("password_hash"));
                }
                return new Passenger(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("id_number"),
                        rs.getString("phone"), rs.getString("username"), rs.getString("password_hash"));
            case "transaction":
                return new Transaction(rs.getInt("transaction_id"), rs.getInt("booking_id"),
                        rs.getString("mpesa_ref_code"), rs.getDouble("amount_paid"),
                        LocalDateTime.parse(rs.getString("payment_date")), rs.getString("status"),
                        rs.getString("payment_method"), rs.getString("card_last4"));
            case "booking":
                Booking b = new Booking(rs.getInt("booking_id"), rs.getInt("passenger_id"), rs.getInt("schedule_id"),
                        rs.getString("origin_station"), rs.getString("destination_station"),
                        rs.getString("coach_number"), rs.getString("seat_number"),
                        LocalDate.parse(rs.getString("travel_date")), rs.getDouble("fare_amount"),
                        rs.getString("booking_status"));
                b.setPassengerName(rs.getString("full_name"));
                return b;
            default:
                throw new IllegalArgumentException("Unknown record type: " + type);
        }
    }

    // ================= small JDBC helpers =================
    private void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) ps.setNull(i + 1, Types.NULL);
            else ps.setObject(i + 1, params[i]);
        }
    }

    private void run(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
        }
    }

    private int insert(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    private DatabaseException wrap(SQLException e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("UNIQUE")) {
            return new DatabaseException("That value is already taken (for example a username, ID number, route or seat).", e);
        }
        if (m.contains("FOREIGN KEY")) {
            return new DatabaseException("This record is linked to other records, so it cannot be changed or deleted.", e);
        }
        return new DatabaseException("Database error: " + m, e);
    }
}
