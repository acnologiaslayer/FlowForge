# FlowForge

A desktop **workflow automation studio** written in Java 21 with a custom,
fully themed Swing GUI. Build workflows from reusable steps (log a message,
set a variable, compute a value, pause, write a file), then run them and watch
live progress stream into a colour-coded run log. Workflows are saved to disk
automatically and reloaded on startup.

This project is an Object-Oriented Programming study piece and deliberately
showcases clean OOP design, a custom GUI, custom exceptions and thorough
JUnit 5 test coverage.

---

## Highlights

- **Custom, themed GUI** - an undecorated window with a hand-painted title
  bar, custom-painted buttons, themed pop-up dialogs and five switchable
  themes (Light, Midnight, Synthwave, Forest, **Cyberpunk**). The Cyberpunk
  theme adds angular neon buttons and bundled display fonts (Orbitron +
  Share Tech Mono).
- **Layered architecture** - `model` / `exception` / `service` /
  `persistence` / `ui` / `gui`, each with a single responsibility and
  depending only on abstractions.
- **SQLite persistence** - workflows are stored in a normalised SQLite
  database (via the xerial JDBC driver) behind a `WorkflowRepository`
  interface, so the storage technology is swappable.
- **Rich custom exceptions** - a `WorkflowException` hierarchy so any layer's
  failure can be caught generically or handled specifically.
- **75 JUnit 5 tests** covering the model, service and persistence layers
  (including the SQLite store and an in-memory repository double).
- **Two front ends over one core** - a Swing GUI (default) and a text
  console (`--console`), proving the domain layer is UI-agnostic.

## OOP concepts demonstrated

| Concept | Where |
|---|---|
| Abstraction | `Task` (abstract base), `WorkflowRepository` (interface) |
| Encapsulation | `Workflow` (private step list, bounds-safe mutators), `Account`-style validated fields |
| Inheritance | `LogTask`, `SetVariableTask`, `ComputeTask`, `DelayTask`, `WriteFileTask` extend `Task` |
| Polymorphism | `WorkflowEngine` runs any `Task` via `execute(...)` without knowing its type |
| Template method | `Task.run(...)` wraps every step's `execute(...)` in uniform error handling |
| Factory | `TaskFactory` builds concrete tasks from persisted fields |
| Programming to interfaces | service layer depends on `WorkflowRepository`, not the concrete SQLite/file class |

## Project layout

```
src/main/java/com/flowforge
├── Main.java                  Entry point (GUI by default, --console for text)
├── exception/                 WorkflowException hierarchy
├── model/                     Workflow, ExecutionContext, RunReport, StepResult
│   └── task/                  Task base + 5 concrete steps + TaskType + TaskFactory
├── service/                   WorkflowManager, WorkflowEngine, listener
├── persistence/               WorkflowRepository + SQLite and file implementations
├── ui/                        ConsoleUI (text front end)
└── gui/                       FlowTheme, FlowForgeApp, custom buttons/title bar/dialogs,
                               Cyberpunk fonts + neon button painting

lib/                           sqlite-jdbc driver
src/test/java/com/flowforge    JUnit 5 tests for model, service, persistence
tools/                         CLI build/test helpers (no Maven/Gradle required)
```

## Running

### From IntelliJ
Open the folder (it has a `pom.xml` and `.iml` with the SQLite library wired
in), then run `com.flowforge.Main`.

### From the command line

```bash
# Compile (SQLite driver on the classpath)
javac -cp "lib/*" -d out/main $(find src/main/java -name '*.java')
# Copy bundled fonts so the Cyberpunk theme can load them
(cd src/main/java && find . -name '*.ttf' -o -name '*.txt' | \
  while read f; do mkdir -p "../../../out/main/$(dirname "$f")"; \
  cp "$f" "../../../out/main/$f"; done)

# Launch the GUI
java -cp "out/main:lib/*" com.flowforge.Main

# Or the text console
java -cp "out/main:lib/*" com.flowforge.Main --console
```

## Testing

Maven/Gradle are not required. `tools/run-tests.sh` compiles everything and
runs the JUnit 5 suite using the JUnit jars already in your local Maven
repository (`~/.m2`):

```bash
bash tools/run-tests.sh
```

It prints a summary and exits non-zero if any test fails. With Maven
installed, `mvn test` works too.

## Data storage

Workflows are persisted in an SQLite database at `data/flowforge.db` using a
normalised three-table schema:

```
workflows(id, name, description, created_at, updated_at)
steps(workflow_id, step_index, type, name)           -> FK to workflows (cascade)
step_fields(workflow_id, step_index, field_key, field_value) -> FK to steps (cascade)
```

Foreign keys with `ON DELETE CASCADE` keep steps and their fields tidy when a
workflow is removed. The storage layer sits behind the `WorkflowRepository`
interface, and a file-based implementation (`FileWorkflowRepository`, `.flow`
text files) is also included; the app auto-migrates any pre-existing `.flow`
files into SQLite on first run.
