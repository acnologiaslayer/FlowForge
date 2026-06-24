package com.flowforge.persistence;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.PersistenceException;
import com.flowforge.model.Workflow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stores each {@link Workflow} as a single {@code .flow} text file inside a
 * directory, using {@link WorkflowSerializer} for the on-disk format.
 * <p>
 * Implements {@link WorkflowRepository}, so callers depend only on the
 * interface and could swap this for a database-backed version later.
 */
public class FileWorkflowRepository implements WorkflowRepository {

    private static final String EXTENSION = ".flow";

    private final Path directory;

    public FileWorkflowRepository(Path directory) throws PersistenceException {
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new PersistenceException(
                    "Could not create workflow directory: " + directory, e);
        }
    }

    @Override
    public List<Workflow> loadAll() throws PersistenceException {
        List<Workflow> workflows = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> flowFiles = files
                    .filter(p -> p.toString().endsWith(EXTENSION))
                    .sorted()
                    .toList();
            for (Path file : flowFiles) {
                workflows.add(readFile(file));
            }
        } catch (IOException e) {
            throw new PersistenceException("Could not list workflows in " + directory, e);
        } catch (UncheckedIOException e) {
            throw new PersistenceException("Could not read a workflow file.", e.getCause());
        }
        return workflows;
    }

    @Override
    public void save(Workflow workflow) throws PersistenceException {
        Path file = fileFor(workflow.getId());
        try {
            Files.writeString(file, WorkflowSerializer.serialize(workflow));
        } catch (IOException e) {
            throw new PersistenceException("Could not save workflow " + workflow.getId(), e);
        }
    }

    @Override
    public void delete(String workflowId) throws PersistenceException {
        try {
            Files.deleteIfExists(fileFor(workflowId));
        } catch (IOException e) {
            throw new PersistenceException("Could not delete workflow " + workflowId, e);
        }
    }

    private Workflow readFile(Path file) throws PersistenceException {
        try {
            return WorkflowSerializer.deserialize(Files.readString(file));
        } catch (IOException e) {
            throw new PersistenceException("Could not read workflow file " + file, e);
        } catch (InvalidTaskConfigurationException e) {
            throw new PersistenceException(
                    "Workflow file " + file + " is corrupt: " + e.getMessage(), e);
        }
    }

    private Path fileFor(String workflowId) {
        return directory.resolve(sanitize(workflowId) + EXTENSION);
    }

    /** Keeps ids filesystem-safe (ids are generated, but be defensive). */
    private static String sanitize(String id) {
        return id.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
