# SGR Rail Management System (Group 4)

Console app in Java + SQLite, following the Group 4 class design.
Payments: M-Pesa or debit/credit card (both simulated; only the card's last 4 digits are stored).

## Run (VS Code)
1. Install the "Extension Pack for Java" (Microsoft). JDK 17+ required.
2. Put `sqlite-jdbc-3.36.0.3.jar` in the `lib` folder.
3. Open this folder in VS Code, open `src/Main.java`, press Run (F5).

## Default admin login
username: `admin`   password: `admin123`   (change it before your demo/submission)

## Notes
- M-Pesa payment is simulated (no real Daraja API call).
- Receipts are saved as text files in `receipts/`.
- Sample trains, routes and schedules are in `DatabaseManager.seed()`; edit to match your brief.
