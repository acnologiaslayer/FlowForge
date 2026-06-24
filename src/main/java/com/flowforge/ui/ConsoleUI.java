package com.flowforge.ui;

import com.flowforge.exception.InvalidTaskConfigurationException;
import com.flowforge.exception.WorkflowException;
import com.flowforge.model.RunReport;
import com.flowforge.model.StepResult;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.SetVariableTask;
import com.flowforge.model.task.Task;
import com.flowforge.service.WorkflowEngine;
import com.flowforge.service.WorkflowManager;

import java.util.List;
import java.util.Scanner;

/**
 * A simple text menu over the same service layer the GUI uses, offered via
 * the {@code --console} flag. It demonstrates that the model/service layers
 * are entirely UI-agnostic: both front ends drive the identical
 * {@link WorkflowManager} and {@link WorkflowEngine}.
 */
public class ConsoleUI {

    private final WorkflowManager manager;
    private final WorkflowEngine engine;
    private final Scanner scanner;

    public ConsoleUI(WorkflowManager manager, WorkflowEngine engine, Scanner scanner) {
        this.manager = manager;
        this.engine = engine;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("=== FlowForge (console) ===");
        boolean running = true;
        while (running) {
            printMenu();
            switch (prompt("Choose an option: ").trim()) {
                case "1" -> listWorkflows();
                case "2" -> createWorkflow();
                case "3" -> addStep();
                case "4" -> runWorkflow();
                case "5" -> deleteWorkflow();
                case "0" -> running = false;
                default -> System.out.println("Unknown option.");
            }
        }
        System.out.println("Goodbye.");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1) List workflows");
        System.out.println("2) Create workflow");
        System.out.println("3) Add a step to a workflow");
        System.out.println("4) Run a workflow");
        System.out.println("5) Delete a workflow");
        System.out.println("0) Exit");
    }

    private void listWorkflows() {
        List<Workflow> workflows = manager.listWorkflows();
        if (workflows.isEmpty()) {
            System.out.println("(no workflows yet)");
            return;
        }
        for (Workflow workflow : workflows) {
            System.out.printf("%-8s %-24s %d step(s)%n",
                    workflow.getId(), workflow.getName(), workflow.stepCount());
            for (Task step : workflow.getSteps()) {
                System.out.println("    - " + step.summary());
            }
        }
    }

    private void createWorkflow() {
        try {
            String name = prompt("Name: ");
            String description = prompt("Description: ");
            Workflow workflow = manager.createWorkflow(name, description);
            System.out.println("Created " + workflow.getId());
        } catch (WorkflowException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void addStep() {
        try {
            Workflow workflow = manager.get(prompt("Workflow id: ").trim());
            System.out.println("Step types: 1) Log  2) Set variable  3) Compute");
            Task task = switch (prompt("Type: ").trim()) {
                case "1" -> new LogTask(prompt("Step name: "), prompt("Message: "));
                case "2" -> new SetVariableTask(prompt("Step name: "),
                        prompt("Variable: "), prompt("Value: "));
                case "3" -> buildCompute();
                default -> throw new InvalidTaskConfigurationException("Unknown step type.");
            };
            workflow.addStep(task);
            manager.save(workflow);
            System.out.println("Added: " + task.summary());
        } catch (WorkflowException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private Task buildCompute() throws InvalidTaskConfigurationException {
        String name = prompt("Step name: ");
        String result = prompt("Result variable: ");
        String left = prompt("Left operand: ");
        String op = prompt("Operator (+ - * /): ");
        String right = prompt("Right operand: ");
        return new ComputeTask(name, result, left,
                ComputeTask.Operator.fromSymbol(op.trim()), right);
    }

    private void runWorkflow() {
        try {
            Workflow workflow = manager.get(prompt("Workflow id: ").trim());
            RunReport report = engine.run(workflow);
            System.out.println("--- Run report: " + report.getWorkflowName() + " ---");
            for (StepResult result : report.getStepResults()) {
                System.out.println(result);
            }
            System.out.printf("Result: %s (%d/%d steps, %d ms)%n",
                    report.isSuccess() ? "SUCCESS" : "FAILED",
                    report.getCompletedSteps(), workflow.stepCount(),
                    report.getTotalDurationMillis());
        } catch (WorkflowException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void deleteWorkflow() {
        try {
            manager.delete(prompt("Workflow id: ").trim());
            System.out.println("Deleted.");
        } catch (WorkflowException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine();
    }
}
