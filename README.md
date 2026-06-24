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
  bar, custom-painted buttons, themed pop-up dialogs and four switchable
  themes (Light, Midnight, Synthwave, Forest).
- **Layered architecture** - `model` / `exception` / `service` /
  `persistence` / `ui` / `gui`, each with a single responsibility and
  depending only on abstractions.
- **Rich custom exceptions** - a `WorkflowException` hierarchy so any layer's
  failure can be caught generically or handled specifically.
- **68 JUnit 5 tests** covering the model, service and persistence layers.
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
| Programming to interfaces | service layer depends on `WorkflowRepository`, not the file class |

## Project layout

```
src/main/java/com/flowforge
├── Main.java                  Entry point (GUI by default, --console for text)
├── exception/                 WorkflowException hierarchy
├── model/                     Workflow, ExecutionContext, RunReport, StepResult
│   └── task/                  Task base + 5 concrete steps + TaskType + TaskFactory
├── service/                   WorkflowManager, WorkflowEngine, listener
├── persistence/               WorkflowRepository, FileWorkflowRepository, serializer
├── ui/                        ConsoleUI (text front end)
└── gui/                       FlowTheme, FlowForgeApp, custom buttons/title bar/dialogs

src/test/java/com/flowforge    JUnit 5 tests for model, service, persistence
tools/                         CLI build/test helpers (no Maven/Gradle required)
```

## Running

### From IntelliJ
Open the folder (it has a `pom.xml` and `.iml`), then run `com.flowforge.Main`.

### From the command line

```bash
# Compile
javac -d out/main $(find src/main/java -name '*.java')

# Launch the GUI
java -cp out/main com.flowforge.Main

# Or the text console
java -cp out/main com.flowforge.Main --console
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

## Data format

Each workflow is stored as a human-readable `data/<id>.flow` text file, for
example:

```
workflow.id=wf-0001
workflow.name=Daily Report
step.0.type=SET
step.0.name=Set greeting
step.0.field.variableName=user
step.0.field.value=Mahir
...
```
