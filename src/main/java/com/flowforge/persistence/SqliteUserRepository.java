package com.flowforge.persistence;

import com.flowforge.exception.PersistenceException;
import com.flowforge.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Optional;

/** SQLite implementation of {@link UserRepository}. */
public class SqliteUserRepository implements UserRepository {

    private final String url;

    public SqliteUserRepository(Path dataDirectory) throws PersistenceException {
        Path databaseFile = dataDirectory.resolve("flowforge.db");
        this.url = "jdbc:sqlite:" + databaseFile;
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new PersistenceException("Could not create data directory " + dataDirectory, e);
        }
        createSchema();
    }

    private void createSchema() throws PersistenceException {
        String users = """
                CREATE TABLE IF NOT EXISTS users (
                    username      TEXT PRIMARY KEY,
                    password_hash TEXT NOT NULL,
                    salt          TEXT NOT NULL,
                    created_at    INTEGER NOT NULL
                );""";
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(users);
        } catch (SQLException e) {
            throw new PersistenceException("Could not initialise user table", e);
        }
    }

    @Override
    public boolean hasUsers() throws PersistenceException {
        String sql = "SELECT 1 FROM users LIMIT 1";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            throw new PersistenceException("Could not check users", e);
        }
    }

    @Override
    public Optional<UserRecord> findByUsername(String username) throws PersistenceException {
        String sql = "SELECT username, password_hash, salt, created_at FROM users WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalize(username));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                User user = new User(rs.getString("username"),
                        Instant.ofEpochMilli(rs.getLong("created_at")));
                return Optional.of(new UserRecord(user,
                        rs.getString("password_hash"), rs.getString("salt")));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not load user '" + username + "'", e);
        }
    }

    @Override
    public User create(String username, String passwordHash, String salt) throws PersistenceException {
        User user = new User(normalize(username), Instant.now());
        String sql = "INSERT INTO users (username, password_hash, salt, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, salt);
            pstmt.setLong(4, user.getCreatedAt().toEpochMilli());
            pstmt.executeUpdate();
            return user;
        } catch (SQLException e) {
            throw new PersistenceException("Could not create user '" + username + "'", e);
        }
    }

    private Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
