# Guess the Word

Guess the Word is a Wordle-style game for Player and Admin users. Players can register, play word games, and see their results. Admins can view daily and per-user reports.

## Tech stack

The project uses Java 17, Spring Boot, Maven, Thymeleaf, Spring JDBC with `JdbcTemplate`, SQLite, and BCrypt from `spring-security-crypto`. It does not use Spring Security.

## How to run

You need JDK 17 or newer. Maven does not need to be installed because the project includes the Maven wrapper.

On Windows, run:

```text
mvnw.cmd spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) in a browser. To use another port, pass `-Dspring-boot.run.arguments=--server.port=18080`, for example:

```text
mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"
```

The `guessword.db` SQLite database is created automatically on first start.

## Default admin account

The default admin username is `Administrator` and the password is `Admin$123`. They are read from `app.admin.username` and `app.admin.password` in `src/main/resources/application.properties`. A real system would not keep a password in a properties file.

## How to use

Players register from the login page and then log in. Press **Play** to start or resume a game. A game allows up to five guesses. Admins log in and open the Daily report or User report from the admin home page.

## Game rules as implemented

The application seeds 20 words. Each user can start three words per day, with five guesses allowed for each word. Green means the letter is in the correct position, orange means it is in the word in another position, and grey means it is not in the word. A win or loss displays a popup with the answer.

## Validation rules

A username must be at least five characters long and contain only letters A-Z or a-z. A password must be at least five characters long and contain a letter, a digit, and at least one of `$`, `%`, or `*`.

## Assumptions

- An unfinished word counts toward the 3-per-day limit and toward "words tried" in the reports, and the player resumes it if they return the same day.
- Guesses are converted to uppercase and must be exactly 5 letters A-Z. They are not checked against a dictionary.
- A "correct guess" in the reports means a game that was won.
- The Daily report counts distinct users who started at least one game that day.
- "Day" is the server's local date.
- The game grid is 5 rows by 5 columns.

## Database

- `users` stores usernames, BCrypt-encoded passwords, roles, and account creation timestamps.
- `words` stores the seeded answer words.
- `game_sessions` stores each user's game, its word, status, and played date.
- `guesses` stores each guess, its number, its result pattern, and creation timestamp.

## Project structure

```text
src/main/java/com/training/guesstheword/
├── config/       # Admin initialization and password encoder configuration
├── controller/   # AuthController, GameController, HomeController, ReportController
├── model/        # User, Word, game state, and report models
├── repository/   # JdbcTemplate repositories
└── service/      # Authentication, game, report, and validation logic
src/main/resources/
├── templates/    # Thymeleaf pages
├── application.properties
├── data.sql
└── schema.sql
src/test/java/com/training/guesstheword/
pom.xml
mvnw.cmd
```

## Running the tests

```text
mvnw.cmd test
```

## Possible improvements

Possible improvements include adding Spring Security, expanding the word list, checking guesses against a dictionary, and storing the admin password in a secrets store.
