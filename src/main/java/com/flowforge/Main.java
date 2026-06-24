package com.flowforge;

import com.flowforge.exception.PersistenceException;
import com.flowforge.exception.WorkflowException;
import com.flowforge.gui.FlowForgeApp;
import com.flowforge.gui.FlowTheme;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.SetVariableTask;
import com.flowforge.model.task.WriteFileTask;
import com.flowforge.persistence.FileWorkflowRepository;
import com.flowforge.persistence.SqliteWorkflowRepository;
import com.flowforge.persistence.WorkflowRepository;
import com.flowforge.service.WorkflowEngine;
import com.flowforge.service.WorkflowManager;
import com.flowforge.ui.ConsoleUI;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Entry point. Wires the layers together
 * (repository -> manager/engine -> UI) and launches the Swing GUI by
 * default. Pass {@code --console} to use the text interface instead.
 */
public class Main {

    public static void main(String[] args) {
        boolean console = args.length > 0 && args[0].equalsIgnoreCase("--console");
        try {
            Path dataDir = Path.of("data");
            WorkflowRepository repository = new SqliteWorkflowRepository(dataDir);
            migrateLegacyFiles(dataDir, repository);
            WorkflowManager manager = new WorkflowManager(repository);
            WorkflowEngine engine = new WorkflowEngine();

            seedExampleIfEmpty(manager);

            if (console) {
                new ConsoleUI(manager, engine, new Scanner(System.in)).run();
            } else {
                SwingUtilities.invokeLater(() -> {
                    installInitialTheme();
                    new FlowForgeApp(manager, engine).setVisible(true);
                });
            }
        } catch (PersistenceException e) {
            System.err.println("Could not start FlowForge: " + e.getMessage());
        }
    }

    private static void installInitialTheme() {
        try {
            FlowTheme.MIDNIGHT.apply();
        } catch (Exception e) {
            System.err.println("Could not install theme: " + e.getMessage());
        }
    }

    /**
     * One-time import of workflows from the old {@code .flow} file store into
     * SQLite, so data created before the switch to SQLite is not lost. Runs
     * only when the database is empty and legacy files exist; the imported
     * files are then removed.
     */
    private static void migrateLegacyFiles(Path dataDir, WorkflowRepository sqlite) {
        try {
            if (!java.nio.file.Files.isDirectory(dataDir)) {
                return;
            }
            boolean hasFlowFiles;
            try (var files = java.nio.file.Files.list(dataDir)) {
                hasFlowFiles = files.anyMatch(p -> p.toString().endsWith(".flow"));
            }
            if (!hasFlowFiles || !sqlite.loadAll().isEmpty()) {
                return;
            }
            FileWorkflowRepository legacy = new FileWorkflowRepository(dataDir);
            for (Workflow workflow : legacy.loadAll()) {
                sqlite.save(workflow);
                legacy.delete(workflow.getId());
            }
            System.out.println("Migrated legacy .flow files into SQLite.");
        } catch (Exception e) {
            System.err.println("Could not migrate legacy workflow files: " + e.getMessage());
        }
    }

    /** Creates a demonstration workflow on first run so the app is not empty. */
    private static void seedExampleIfEmpty(WorkflowManager manager) {
        if (manager.count() > 0) {
            return;
        }
        try {
            Workflow demo = manager.createWorkflow("Daily Report",
                    "A sample workflow showing variables, computation and file output.");
            demo.addStep(new SetVariableTask("Set greeting", "user", "Mahir"));
            demo.addStep(new LogTask("Greet", "Starting report for ${user}..."));
            demo.addStep(new SetVariableTask("Morning sales", "morning", "1200"));
            demo.addStep(new SetVariableTask("Evening sales", "evening", "1850"));
            demo.addStep(new ComputeTask("Total sales", "total",
                    "${morning}", ComputeTask.Operator.ADD, "${evening}"));
            demo.addStep(new LogTask("Report total", "Total sales for ${user}: ${total}"));
            demo.addStep(new WriteFileTask("Save report", "data/daily-report.txt",
                    "Report for ${user}\nMorning: ${morning}\nEvening: ${evening}\nTotal: ${total}",
                    false));
            manager.save(demo);
        } catch (WorkflowException e) {
            System.err.println("Could not seed example workflow: " + e.getMessage());
        }
    }
}
