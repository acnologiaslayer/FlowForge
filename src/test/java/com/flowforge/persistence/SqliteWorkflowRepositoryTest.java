package com.flowforge.persistence;

import com.flowforge.exception.PersistenceException;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.SetVariableTask;
import com.flowforge.model.task.WriteFileTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SqliteWorkflowRepository} using a JUnit {@link TempDir}
 * so a throwaway database file is created per test class: save, reload,
 * overwrite, delete and full task round-tripping through the normalised
 * schema.
 */
class SqliteWorkflowRepositoryTest {

    @TempDir
    Path tempDir;

    private SqliteWorkflowRepository repository;

    @BeforeEach
    void setUp() throws PersistenceException {
        repository = new SqliteWorkflowRepository(tempDir);
    }

    private Workflow sample(String id, String name) throws Exception {
        Workflow workflow = new Workflow(id, name, "desc");
        workflow.addStep(new LogTask("Greet", "hello ${user}"));
        workflow.addStep(new SetVariableTask("Set", "user", "Mahir"));
        workflow.addStep(new ComputeTask("Sum", "total", "1",
                ComputeTask.Operator.ADD, "2"));
        workflow.addStep(new WriteFileTask("Save", "out.txt", "Total: ${total}", true));
        return workflow;
    }

    @Test
    void freshDatabaseIsEmpty() throws Exception {
        assertTrue(repository.loadAll().isEmpty());
    }

    @Test
    void savedWorkflowReloadsWithAllSteps() throws Exception {
        repository.save(sample("wf-1", "First"));

        List<Workflow> loaded = repository.loadAll();
        assertEquals(1, loaded.size());
        Workflow restored = loaded.get(0);
        assertEquals("First", restored.getName());
        assertEquals(4, restored.stepCount());
        assertEquals("Greet", restored.getStep(0).getName());
        assertEquals("Compute", restored.getStep(2).getType().getLabel());
    }

    @Test
    void taskFieldsSurviveRoundTrip() throws Exception {
        repository.save(sample("wf-1", "First"));
        Workflow restored = repository.loadAll().get(0);

        WriteFileTask write = (WriteFileTask) restored.getStep(3);
        assertEquals("out.txt", write.getPath());
        assertEquals("Total: ${total}", write.getContent());
        assertTrue(write.isAppend());
    }

    @Test
    void saveOverwritesExistingWorkflow() throws Exception {
        repository.save(sample("wf-1", "Original"));
        repository.save(sample("wf-1", "Renamed"));

        List<Workflow> loaded = repository.loadAll();
        assertEquals(1, loaded.size());
        assertEquals("Renamed", loaded.get(0).getName());
        assertEquals(4, loaded.get(0).stepCount()); // steps replaced, not duplicated
    }

    @Test
    void deleteRemovesWorkflowAndCascadesSteps() throws Exception {
        repository.save(sample("wf-1", "First"));
        repository.delete("wf-1");
        assertTrue(repository.loadAll().isEmpty());

        // Re-saving the same id must not collide with orphaned step rows.
        repository.save(sample("wf-1", "Recreated"));
        assertEquals(4, repository.loadAll().get(0).stepCount());
    }

    @Test
    void multipleWorkflowsAreLoaded() throws Exception {
        repository.save(sample("wf-1", "A"));
        repository.save(sample("wf-2", "B"));
        assertEquals(2, repository.loadAll().size());
    }

    @Test
    void timestampsArePreserved() throws Exception {
        Workflow workflow = sample("wf-1", "First");
        long created = workflow.getCreatedAt().toEpochMilli();
        repository.save(workflow);

        Workflow restored = repository.loadAll().get(0);
        assertEquals(created, restored.getCreatedAt().toEpochMilli());
    }
}
