package com.flowforge.service;

import com.flowforge.model.ExecutionContext;
import com.flowforge.model.RunReport;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.DelayTask;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests that {@link WorkflowExecutionService} actually executes workflows concurrently. */
class WorkflowExecutionServiceTest {

    @Test
    void runsTwoWorkflowsConcurrentlyOnBackgroundThreads() throws Exception {
        WorkflowExecutionService service = new WorkflowExecutionService(new WorkflowEngine(), 2);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        WorkflowExecutionListener listener = new WorkflowExecutionListener() {
            @Override
            public void onStepStarted(int index, Workflow workflow) {
                threadNames.add(Thread.currentThread().getName());
            }
        };

        long start = System.nanoTime();
        var first = service.runAsync(delayed("A"), new ExecutionContext(), listener);
        var second = service.runAsync(delayed("B"), new ExecutionContext(), listener);

        RunReport a = first.get(2, TimeUnit.SECONDS);
        RunReport b = second.get(2, TimeUnit.SECONDS);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        service.close();

        assertTrue(a.isSuccess());
        assertTrue(b.isSuccess());
        assertTrue(elapsedMillis < 550,
                "two 300ms workflows should overlap on the 2-thread pool, elapsed=" + elapsedMillis);
        assertEquals(2, threadNames.size());
        assertTrue(threadNames.stream().allMatch(name -> name.startsWith("flowforge-runner-")));
    }

    @Test
    void validationFailureCompletesFutureExceptionally() throws Exception {
        WorkflowExecutionService service = new WorkflowExecutionService(new WorkflowEngine(), 1);
        Workflow empty = new Workflow("empty", "Empty", "");
        var future = service.runAsync(empty, new ExecutionContext(), null);
        try {
            future.get(1, TimeUnit.SECONDS);
            throw new AssertionError("Expected future to fail");
        } catch (java.util.concurrent.ExecutionException e) {
            assertTrue(e.getCause() instanceof WorkflowExecutionService.WorkflowRunException);
        } finally {
            service.close();
        }
    }

    private Workflow delayed(String id) throws Exception {
        Workflow workflow = new Workflow(id, id, "");
        workflow.addStep(new DelayTask("wait", 300));
        return workflow;
    }
}
