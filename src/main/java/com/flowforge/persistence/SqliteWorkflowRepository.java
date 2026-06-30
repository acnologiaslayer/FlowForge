package com.flowforge.persistence;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.PersistenceException;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.Task;
import com.flowforge.model.task.TaskFactory;
import com.flowforge.model.task.TaskType;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow store backed by an SQLite database (via the xerial JDBC driver).
 * <p>
 * The schema is normalised across workflows/steps/step_fields and now includes
 * an optional {@code owner_username} column. Constructing this repository with
 * a username activates multi-user mode: load/save/delete operations are scoped
 * to that owner, so different users can share one database safely.
 */
public class SqliteWorkflowRepository implements WorkflowRepository {

    private final String url;
    private final String ownerUsername;

    /** Legacy/admin constructor: sees all workflows and writes unowned rows. */
    public SqliteWorkflowRepository(Path dataDirectory) throws PersistenceException {
        this(dataDirectory, null);
    }

    /** User-scoped constructor: sees and writes only workflows owned by username. */
    public SqliteWorkflowRepository(Path dataDirectory, String ownerUsername)
            throws PersistenceException {
        Path databaseFile = dataDirectory.resolve("flowforge.db");
        this.url = "jdbc:sqlite:" + databaseFile;
        this.ownerUsername = normalizeOwner(ownerUsername);
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new PersistenceException("Could not create data directory " + dataDirectory, e);
        }
        createSchema();
    }

    // ---------- schema ----------

    private void createSchema() throws PersistenceException {
        String users = """
                CREATE TABLE IF NOT EXISTS users (
                    username      TEXT PRIMARY KEY,
                    password_hash TEXT NOT NULL,
                    salt          TEXT NOT NULL,
                    created_at    INTEGER NOT NULL
                );""";
        String workflows = """
                CREATE TABLE IF NOT EXISTS workflows (
                    id             TEXT PRIMARY KEY,
                    owner_username TEXT,
                    name           TEXT NOT NULL,
                    description    TEXT NOT NULL,
                    created_at     INTEGER NOT NULL,
                    updated_at     INTEGER NOT NULL,
                    FOREIGN KEY (owner_username) REFERENCES users(username) ON DELETE CASCADE
                );""";
        String steps = """
                CREATE TABLE IF NOT EXISTS steps (
                    workflow_id TEXT NOT NULL,
                    step_index  INTEGER NOT NULL,
                    type        TEXT NOT NULL,
                    name        TEXT NOT NULL,
                    PRIMARY KEY (workflow_id, step_index),
                    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE
                );""";
        String stepFields = """
                CREATE TABLE IF NOT EXISTS step_fields (
                    workflow_id TEXT NOT NULL,
                    step_index  INTEGER NOT NULL,
                    field_key   TEXT NOT NULL,
                    field_value TEXT NOT NULL,
                    PRIMARY KEY (workflow_id, step_index, field_key),
                    FOREIGN KEY (workflow_id, step_index)
                        REFERENCES steps(workflow_id, step_index) ON DELETE CASCADE
                );""";
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(users);
            stmt.execute(workflows);
            addOwnerColumnIfMissing(conn);
            stmt.execute(steps);
            stmt.execute(stepFields);
        } catch (SQLException e) {
            throw new PersistenceException("Could not initialise the database", e);
        }
    }

    /** Adds owner_username for databases created before multi-user support. */
    private void addOwnerColumnIfMissing(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("PRAGMA table_info(workflows)")) {
            while (rs.next()) {
                if ("owner_username".equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE workflows ADD COLUMN owner_username TEXT");
        }
    }

    // ---------- read ----------

    @Override
    public List<Workflow> loadAll() throws PersistenceException {
        List<Workflow> workflows = new ArrayList<>();
        String sql = "SELECT id, name, description, created_at, updated_at FROM workflows "
                + (isUserScoped() ? "WHERE owner_username = ? " : "")
                + "ORDER BY id";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (isUserScoped()) {
                pstmt.setString(1, ownerUsername);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Workflow workflow = new Workflow(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            Instant.ofEpochMilli(rs.getLong("created_at")),
                            Instant.ofEpochMilli(rs.getLong("updated_at")));
                    loadSteps(conn, workflow);
                    workflows.add(workflow);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not load workflows from the database", e);
        } catch (InvalidTaskConfigurationException e) {
            throw new PersistenceException("A stored workflow is corrupt: " + e.getMessage(), e);
        }
        return workflows;
    }

    private void loadSteps(Connection conn, Workflow workflow)
            throws SQLException, InvalidTaskConfigurationException {
        String sql = "SELECT step_index, type, name FROM steps "
                + "WHERE workflow_id = ? ORDER BY step_index";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workflow.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int index = rs.getInt("step_index");
                    TaskType type = TaskType.fromCode(rs.getString("type"));
                    String name = rs.getString("name");
                    Map<String, String> fields = loadFields(conn, workflow.getId(), index);
                    workflow.addStep(TaskFactory.create(type, name, fields));
                }
            }
        }
    }

    private Map<String, String> loadFields(Connection conn, String workflowId, int stepIndex)
            throws SQLException {
        Map<String, String> fields = new LinkedHashMap<>();
        String sql = "SELECT field_key, field_value FROM step_fields "
                + "WHERE workflow_id = ? AND step_index = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workflowId);
            pstmt.setInt(2, stepIndex);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    fields.put(rs.getString("field_key"), rs.getString("field_value"));
                }
            }
        }
        return fields;
    }

    // ---------- write ----------

    @Override
    public void save(Workflow workflow) throws PersistenceException {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try {
                upsertWorkflow(conn, workflow);
                deleteSteps(conn, workflow.getId());
                insertSteps(conn, workflow);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not save workflow " + workflow.getId(), e);
        }
    }

    private void upsertWorkflow(Connection conn, Workflow workflow) throws SQLException {
        String sql = "INSERT OR REPLACE INTO workflows "
                + "(id, owner_username, name, description, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workflow.getId());
            pstmt.setString(2, ownerUsername);
            pstmt.setString(3, workflow.getName());
            pstmt.setString(4, workflow.getDescription());
            pstmt.setLong(5, workflow.getCreatedAt().toEpochMilli());
            pstmt.setLong(6, workflow.getUpdatedAt().toEpochMilli());
            pstmt.executeUpdate();
        }
    }

    private void insertSteps(Connection conn, Workflow workflow) throws SQLException {
        String stepSql = "INSERT INTO steps (workflow_id, step_index, type, name) "
                + "VALUES (?, ?, ?, ?)";
        String fieldSql = "INSERT INTO step_fields "
                + "(workflow_id, step_index, field_key, field_value) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stepStmt = conn.prepareStatement(stepSql);
             PreparedStatement fieldStmt = conn.prepareStatement(fieldSql)) {
            List<Task> steps = workflow.getSteps();
            for (int i = 0; i < steps.size(); i++) {
                Task task = steps.get(i);
                stepStmt.setString(1, workflow.getId());
                stepStmt.setInt(2, i);
                stepStmt.setString(3, task.getType().getCode());
                stepStmt.setString(4, task.getName());
                stepStmt.executeUpdate();

                for (Map.Entry<String, String> field : task.toFields().entrySet()) {
                    fieldStmt.setString(1, workflow.getId());
                    fieldStmt.setInt(2, i);
                    fieldStmt.setString(3, field.getKey());
                    fieldStmt.setString(4, field.getValue());
                    fieldStmt.executeUpdate();
                }
            }
        }
    }

    private void deleteSteps(Connection conn, String workflowId) throws SQLException {
        try (PreparedStatement pstmt =
                     conn.prepareStatement("DELETE FROM steps WHERE workflow_id = ?")) {
            pstmt.setString(1, workflowId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(String workflowId) throws PersistenceException {
        String sql = "DELETE FROM workflows WHERE id = ?" +
                (isUserScoped() ? " AND owner_username = ?" : "");
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workflowId);
            if (isUserScoped()) {
                pstmt.setString(2, ownerUsername);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Could not delete workflow " + workflowId, e);
        }
    }

    // ---------- helpers ----------

    private Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    private boolean isUserScoped() {
        return ownerUsername != null && !ownerUsername.isBlank();
    }

    private static String normalizeOwner(String ownerUsername) {
        if (ownerUsername == null || ownerUsername.isBlank()) {
            return null;
        }
        return ownerUsername.trim().toLowerCase();
    }
}
