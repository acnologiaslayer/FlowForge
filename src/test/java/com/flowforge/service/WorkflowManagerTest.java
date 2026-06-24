package com.flowforge.service;

import com.flowforge.exception.PersistenceException;
import com.flowforge.exception.WorkflowNotFoundException;
import com.flowforge.exception.WorkflowValidationException;
import com.flowforge.model.Workflow;
import com.flowforge.persistence.WorkflowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link WorkflowManager} using a fake in-memory
 * {@link WorkflowRepository}. This both isolates the test from the file
 * system and demonstrates the benefit of depending on the repository
 * interface rather than the concrete file implementation.
 */
class WorkflowManagerTest {

    /** A simple in-memory repository double for the tests. */
    private static class InMemoryRepository implements WorkflowRepository {
        private final Map<String, Workflow> store = new LinkedHashMap<>();

        @Override
        public List<Workflow> loadAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void save(Workflow workflow) {
            store.put(workflow.getId(), workflow);
        }

        @Override
        public void delete(String workflowId) {
            store.remove(workflowId);
        }

        int size() {
            return store.size();
        }
    }

    private InMemoryRepository repository;
    private WorkflowManager manager;

    @BeforeEach
    void setUp() throws PersistenceException {
        repository = new InMemoryRepository();
        manager = new WorkflowManager(repository);
    }

    @Test
    void createWorkflowPersistsAndReturnsIt() throws Exception {
        Workflow workflow = manager.createWorkflow("First", "desc");
        assertEquals("First", workflow.getName());
        assertEquals(1, manager.count());
        assertEquals(1, repository.size());
    }

    @Test
    void createWorkflowRejectsBlankName() {
        assertThrows(WorkflowValidationException.class,
                () -> manager.createWorkflow("  ", "desc"));
    }

    @Test
    void getReturnsCreatedWorkflow() throws Exception {
        Workflow created = manager.createWorkflow("First", "");
        assertEquals(created.getId(), manager.get(created.getId()).getId());
    }

    @Test
    void getUnknownIdThrows() {
        assertThrows(WorkflowNotFoundException.class, () -> manager.get("nope"));
    }

    @Test
    void deleteRemovesFromMemoryAndStore() throws Exception {
        Workflow created = manager.createWorkflow("First", "");
        manager.delete(created.getId());
        assertEquals(0, manager.count());
        assertEquals(0, repository.size());
    }

    @Test
    void deleteUnknownThrows() {
        assertThrows(WorkflowNotFoundException.class, () -> manager.delete("nope"));
    }

    @Test
    void idsAreUniqueAndIncrementing() throws Exception {
        Workflow a = manager.createWorkflow("A", "");
        Workflow b = manager.createWorkflow("B", "");
        assertFalse(a.getId().equals(b.getId()));
    }

    @Test
    void listWorkflowsIsSortedByName() throws Exception {
        manager.createWorkflow("Zebra", "");
        manager.createWorkflow("Apple", "");
        List<Workflow> list = manager.listWorkflows();
        assertEquals("Apple", list.get(0).getName());
        assertEquals("Zebra", list.get(1).getName());
    }

    @Test
    void existingWorkflowsAreLoadedAtStartup() throws Exception {
        Workflow seeded = new Workflow("wf-5", "Seeded", "");
        repository.save(seeded);
        WorkflowManager reloaded = new WorkflowManager(repository);
        assertEquals(1, reloaded.count());
        assertTrue(reloaded.get("wf-5") != null);
    }

    @Test
    void saveUnknownWorkflowThrows() {
        Workflow detached = new Workflow("wf-99", "Detached", "");
        assertThrows(WorkflowNotFoundException.class, () -> manager.save(detached));
    }
}
