package com.flowforge.persistence;

import com.flowforge.exception.PersistenceException;
import com.flowforge.model.Workflow;

import java.util.List;

/**
 * Abstraction over wherever workflows are stored.
 * <p>
 * The service layer depends on this interface rather than a concrete file
 * or database class, so the storage mechanism can be swapped (file, SQLite,
 * in-memory for tests) without touching the rest of the application.
 */
public interface WorkflowRepository {

    /** Loads every stored workflow. */
    List<Workflow> loadAll() throws PersistenceException;

    /** Saves (creates or overwrites) a workflow. */
    void save(Workflow workflow) throws PersistenceException;

    /** Deletes the workflow with the given id, if it exists. */
    void delete(String workflowId) throws PersistenceException;
}
