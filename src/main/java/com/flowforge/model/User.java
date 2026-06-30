package com.flowforge.model;

import java.time.Instant;

/**
 * A FlowForge account. Workflows are scoped to the authenticated user, so two
 * people can use the same SQLite database without seeing or overwriting each
 * other's workflows.
 */
public class User {

    private final String username;
    private final Instant createdAt;

    public User(String username, Instant createdAt) {
        this.username = username;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getUsername() {
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return username;
    }
}
