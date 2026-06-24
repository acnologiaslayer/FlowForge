package com.flowforge.persistence;

import com.flowforge.exception.PersistenceException;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.LogTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FileWorkflowRepository} using a JUnit {@link TempDir} so
 * no real files are touched: save, reload, overwrite and delete.
 */
class FileWorkflowRepositoryTest {

    @TempDir
    Path tempDir;

    private FileWorkflowRepository repository;

    @BeforeEach
    void setUp() throws PersistenceException {
        repository = new FileWorkflowRepository(tempDir);
    }

    private Workflow sample(String id, String name) throws Exception {
        Workflow workflow = new Workflow(id, name, "desc");
        workflow.addStep(new LogTask("Greet", "hello"));
        return workflow;
    }

    @Test
    void savedWorkflowCanBeReloaded() throws Exception {
        repository.save(sample("wf-1", "First"));

        List<Workflow> loaded = repository.loadAll();
        assertEquals(1, loaded.size());
        assertEquals("First", loaded.get(0).getName());
        assertEquals(1, loaded.get(0).stepCount());
    }

    @Test
    void loadAllReturnsEmptyForFreshDirectory() throws Exception {
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void saveOverwritesExistingWorkflow() throws Exception {
        repository.save(sample("wf-1", "Original"));
        repository.save(sample("wf-1", "Renamed"));

        List<Workflow> loaded = repository.loadAll();
        assertEquals(1, loaded.size());
        assertEquals("Renamed", loaded.get(0).getName());
    }

    @Test
    void deleteRemovesWorkflow() throws Exception {
        repository.save(sample("wf-1", "First"));
        repository.delete("wf-1");
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void deleteMissingWorkflowIsSilent() throws Exception {
        repository.delete("does-not-exist"); // should not throw
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void multipleWorkflowsAreLoaded() throws Exception {
        repository.save(sample("wf-1", "A"));
        repository.save(sample("wf-2", "B"));
        assertEquals(2, repository.loadAll().size());
    }
}
