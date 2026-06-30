package com.flowforge.service;

import com.flowforge.exception.DuplicateWorkflowException;
import com.flowforge.exception.PersistenceException;
import com.flowforge.exception.WorkflowException;
import com.flowforge.exception.WorkflowNotFoundException;
import com.flowforge.model.Workflow;
import com.flowforge.persistence.WorkflowRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application service that manages the collection of workflows: creating,
 * loading, saving and deleting them.
 * <p>
 * It keeps an in-memory cache (keyed by id) backed by a
 * {@link WorkflowRepository}, generates unique ids, and exposes a small,
 * intention-revealing API to the UI. The UI never talks to the repository
 * directly, which keeps persistence concerns in one place.
 */
public class WorkflowManager {

    private final WorkflowRepository repository;
    private final Map<String, Workflow> workflows = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(0);
    private final String idPrefix;

    public WorkflowManager(WorkflowRepository repository) throws PersistenceException {
        this(repository, "wf");
    }

    public WorkflowManager(WorkflowRepository repository, String ownerUsername)
            throws PersistenceException {
        this.repository = repository;
        this.idPrefix = sanitizePrefix(ownerUsername);
        for (Workflow workflow : repository.loadAll()) {
            workflows.put(workflow.getId(), workflow);
            trackSequence(workflow.getId());
        }
    }

    /** Creates, stores and returns a new empty workflow. */
    public Workflow createWorkflow(String name, String description) throws WorkflowException {
        if (name == null || name.isBlank()) {
            throw new com.flowforge.exception.WorkflowValidationException(
                    "Workflow name must not be blank.");
        }
        String id = nextId();
        if (workflows.containsKey(id)) {
            throw new DuplicateWorkflowException(id);
        }
        Workflow workflow = new Workflow(id, name.trim(), description);
        workflows.put(id, workflow);
        repository.save(workflow);
        return workflow;
    }

    /** Persists changes made to an existing workflow. */
    public void save(Workflow workflow) throws WorkflowException {
        if (!workflows.containsKey(workflow.getId())) {
            throw new WorkflowNotFoundException(workflow.getId());
        }
        repository.save(workflow);
    }

    /** Returns the workflow with the given id or throws if it is unknown. */
    public Workflow get(String workflowId) throws WorkflowNotFoundException {
        Workflow workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new WorkflowNotFoundException(workflowId);
        }
        return workflow;
    }

    /** Removes a workflow from memory and storage. */
    public void delete(String workflowId) throws WorkflowException {
        if (!workflows.containsKey(workflowId)) {
            throw new WorkflowNotFoundException(workflowId);
        }
        repository.delete(workflowId);
        workflows.remove(workflowId);
    }

    /** All workflows, sorted by name for stable display. */
    public List<Workflow> listWorkflows() {
        List<Workflow> all = new ArrayList<>(workflows.values());
        all.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return all;
    }

    public int count() {
        return workflows.size();
    }

    // ---------- helpers ----------

    private String nextId() {
        return String.format("%s-%04d", idPrefix, sequence.incrementAndGet());
    }

    /** Keeps the id sequence ahead of any ids loaded from disk. */
    private void trackSequence(String id) {
        String prefix = idPrefix + "-";
        if (id != null && id.startsWith(prefix)) {
            try {
                int value = Integer.parseInt(id.substring(prefix.length()));
                sequence.accumulateAndGet(value, Math::max);
            } catch (NumberFormatException ignored) {
                // non-standard id; leave the sequence untouched
            }
        }
    }

    private static String sanitizePrefix(String username) {
        String normalized = username == null ? "wf" : username.trim().toLowerCase();
        if (normalized.isBlank() || normalized.equals("wf")) {
            return "wf";
        }
        return "wf-" + normalized.replaceAll("[^a-z0-9_]+", "_");
    }
}
