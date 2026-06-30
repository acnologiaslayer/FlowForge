package com.flowforge.persistence;

import com.flowforge.model.Workflow;
import com.flowforge.model.task.LogTask;
import com.flowforge.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests SQLite users plus user-scoped workflow visibility. */
class MultiUserSqliteTest {

    @TempDir
    Path tempDir;

    @Test
    void userRepositoryPersistsUsers() throws Exception {
        SqliteUserRepository users = new SqliteUserRepository(tempDir);
        AuthService auth = new AuthService(users);

        assertFalse(users.hasUsers());
        auth.register("alice", "secret1".toCharArray());
        assertTrue(users.hasUsers());
        assertEquals("alice", auth.login("alice", "secret1".toCharArray()).getUsername());

        // Prove credentials survive a new repository instance.
        AuthService reloaded = new AuthService(new SqliteUserRepository(tempDir));
        assertEquals("alice", reloaded.login("alice", "secret1".toCharArray()).getUsername());
    }

    @Test
    void scopedWorkflowRepositoriesOnlySeeTheirOwnersWorkflows() throws Exception {
        AuthService auth = new AuthService(new SqliteUserRepository(tempDir));
        auth.register("alice", "secret1".toCharArray());
        auth.register("bob", "secret2".toCharArray());

        SqliteWorkflowRepository aliceRepo = new SqliteWorkflowRepository(tempDir, "alice");
        SqliteWorkflowRepository bobRepo = new SqliteWorkflowRepository(tempDir, "bob");

        Workflow alice = workflow("wf-alice-0001", "Alice Flow");
        Workflow bob = workflow("wf-bob-0001", "Bob Flow");
        aliceRepo.save(alice);
        bobRepo.save(bob);

        assertEquals(1, aliceRepo.loadAll().size());
        assertEquals("Alice Flow", aliceRepo.loadAll().get(0).getName());
        assertEquals(1, bobRepo.loadAll().size());
        assertEquals("Bob Flow", bobRepo.loadAll().get(0).getName());

        aliceRepo.delete(bob.getId());
        assertEquals(1, bobRepo.loadAll().size(), "Alice must not be able to delete Bob's flow");
    }

    @Test
    void legacyRepositoryStillSeesAllWorkflows() throws Exception {
        AuthService auth = new AuthService(new SqliteUserRepository(tempDir));
        auth.register("alice", "secret1".toCharArray());
        auth.register("bob", "secret2".toCharArray());

        new SqliteWorkflowRepository(tempDir, "alice").save(workflow("wf-alice-0001", "A"));
        new SqliteWorkflowRepository(tempDir, "bob").save(workflow("wf-bob-0001", "B"));

        assertEquals(2, new SqliteWorkflowRepository(tempDir).loadAll().size());
    }

    private Workflow workflow(String id, String name) throws Exception {
        Workflow workflow = new Workflow(id, name, "");
        workflow.addStep(new LogTask("log", "hello"));
        return workflow;
    }
}
