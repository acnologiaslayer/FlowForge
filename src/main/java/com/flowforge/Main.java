package com.flowforge;

import com.flowforge.exception.AuthenticationException;
import com.flowforge.exception.PersistenceException;
import com.flowforge.exception.WorkflowException;
import com.flowforge.gui.FlowForgeApp;
import com.flowforge.gui.FlowTheme;
import com.flowforge.gui.LoginDialog;
import com.flowforge.model.User;
import com.flowforge.model.Workflow;
import com.flowforge.model.task.ComputeTask;
import com.flowforge.model.task.Condition;
import com.flowforge.model.task.EndIfTask;
import com.flowforge.model.task.EndLoopTask;
import com.flowforge.model.task.HttpRequestTask;
import com.flowforge.model.task.IfTask;
import com.flowforge.model.task.JsonExtractTask;
import com.flowforge.model.task.LogTask;
import com.flowforge.model.task.LoopTask;
import com.flowforge.model.task.SetVariableTask;
import com.flowforge.model.task.WriteFileTask;
import com.flowforge.persistence.FileWorkflowRepository;
import com.flowforge.persistence.SqliteUserRepository;
import com.flowforge.persistence.SqliteWorkflowRepository;
import com.flowforge.persistence.WorkflowRepository;
import com.flowforge.service.AuthService;
import com.flowforge.service.WorkflowEngine;
import com.flowforge.service.WorkflowExecutionService;
import com.flowforge.service.WorkflowManager;
import com.flowforge.ui.ConsoleUI;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Entry point. Wires the layers together
 * (repository -> manager/engine -> UI) and launches the Swing GUI by default.
 * Pass {@code --console} to use the text interface instead.
 */
public class Main {

    public static void main(String[] args) {
        boolean console = args.length > 0 && args[0].equalsIgnoreCase("--console");
        try {
            Path dataDir = Path.of("data");
            WorkflowRepository legacyRepository = new SqliteWorkflowRepository(dataDir);
            migrateLegacyFiles(dataDir, legacyRepository);

            AuthService authService = new AuthService(new SqliteUserRepository(dataDir));
            WorkflowEngine engine = new WorkflowEngine();

            if (console) {
                Scanner scanner = new Scanner(System.in);
                User user = authenticateConsole(authService, scanner);
                WorkflowRepository repository = new SqliteWorkflowRepository(dataDir, user.getUsername());
                WorkflowManager manager = new WorkflowManager(repository, user.getUsername());
                seedExampleIfEmpty(manager);
                new ConsoleUI(manager, engine, scanner).run();
            } else {
                SwingUtilities.invokeLater(() -> launchGui(dataDir, authService, engine));
            }
        } catch (PersistenceException e) {
            System.err.println("Could not start FlowForge: " + e.getMessage());
        }
    }

    private static void launchGui(Path dataDir, AuthService authService, WorkflowEngine engine) {
        try {
            installInitialTheme();
            User user = new LoginDialog(authService).show(null);
            if (user == null) {
                System.exit(0);
            }
            WorkflowRepository repository = new SqliteWorkflowRepository(dataDir, user.getUsername());
            WorkflowManager manager = new WorkflowManager(repository, user.getUsername());
            seedExampleIfEmpty(manager);
            WorkflowExecutionService executionService = new WorkflowExecutionService(engine);
            FlowForgeApp app = new FlowForgeApp(manager, executionService, user);
            // On logout, the window has already disposed itself (shutting down
            // its thread pool); reopen the login screen for the next user.
            app.setOnLogout(() -> launchGui(dataDir, authService, engine));
            app.setVisible(true);
        } catch (PersistenceException e) {
            System.err.println("Could not open FlowForge: " + e.getMessage());
        }
    }

    private static User authenticateConsole(AuthService authService, Scanner scanner)
            throws PersistenceException {
        while (true) {
            System.out.println("=== FlowForge Login ===");
            System.out.print("Login or register? [l/r]: ");
            String mode = scanner.nextLine().trim().toLowerCase();
            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Password: ");
            char[] password = scanner.nextLine().toCharArray();
            try {
                if (mode.startsWith("r") || !authService.hasUsers()) {
                    return authService.register(username, password);
                }
                return authService.login(username, password);
            } catch (AuthenticationException e) {
                System.out.println(e.getMessage());
            }
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

    /** Creates demonstration workflows per user on first login so the app is not empty. */
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

            seedApiWorkflow(manager);
        } catch (WorkflowException e) {
            System.err.println("Could not seed example workflow: " + e.getMessage());
        }
    }

    private static void seedApiWorkflow(WorkflowManager manager) throws WorkflowException {
        Workflow api = manager.createWorkflow("API Health Check",
                "Calls a public API, inspects the JSON, branches on the result and loops.");
        api.addStep(new HttpRequestTask("Fetch post", HttpRequestTask.Method.GET,
                "https://jsonplaceholder.typicode.com/posts/1", "",
                java.util.Map.of("Accept", "application/json"), "response"));
        api.addStep(new JsonExtractTask("Read title", "${response_body}", "title", "title"));
        api.addStep(new IfTask("Has title?",
                new Condition("${title}", Condition.Comparator.IS_NOT_EMPTY, "")));
        api.addStep(new LogTask("Log title", "Fetched title: ${title}"));
        api.addStep(new EndIfTask("End check"));
        api.addStep(LoopTask.count("Repeat", "3", "i"));
        api.addStep(new LogTask("Loop log", "Pass number ${i}"));
        api.addStep(new EndLoopTask("End repeat"));
        manager.save(api);
    }
}
