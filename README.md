# FlowForge

A desktop **workflow automation studio** written in Java 21 with a custom,
fully themed Swing GUI. Build workflows from reusable steps, then run them and
watch live progress stream into a colour-coded run log. Inspired by tools like
n8n, FlowForge goes beyond a linear script: it calls **HTTP** endpoints, parses
**JSON** responses, branches with **IF / ELSE**, and repeats work with counted
and conditional **loops**. Workflows are saved to disk automatically and
reloaded on startup.

This project is an Object-Oriented Programming study piece and deliberately
showcases clean OOP design, a custom GUI, custom exceptions and thorough
JUnit 5 test coverage.

---

## Step types

| Step | What it does |
|---|---|
| **Log Message** | Print a message (supports `${variable}` placeholders) |
| **Set Variable** | Store a value for later steps |
| **Compute** | Arithmetic on two operands into a result variable |
| **Delay** | Pause for a number of milliseconds |
| **Write File** | Write text content to disk |
| **HTTP Request** | Call a GET/POST/PUT/DELETE/PATCH endpoint; stores status + body |
| **JSON Extract** | Parse JSON and pull a value out by path (`data.items.0.name`) |
| **If / Else / End If** | Conditional branching |
| **Loop / End Loop** | Repeat a block a fixed number of times or while a condition holds |

## Highlights

- **Multi-user accounts** - a login/register screen gates the app. Passwords
  are salted and hashed with PBKDF2 (never stored in plain text), and every
  workflow is scoped to its owner, so multiple people share one database
  without seeing or overwriting each other's workflows.
- **Multithreaded execution** - workflows run on a dedicated fixed thread pool
  (`WorkflowExecutionService`) using named daemon threads, so the Swing UI
  stays responsive and several workflows can run at the same time.
- **n8n-style control flow** - IF/ELSE branching and counted/while loops, built
  on the **Composite pattern**: the flat step list is compiled into a tree of
  `FlowNode`s (`SequenceNode`, `IfNode`, `LoopNode`, `TaskNode`) that a
  recursive interpreter walks. Marker steps keep storage and the GUI simple
  while delivering real nested control flow.
- **HTTP + JSON, dependency-free** - the HTTP step uses the JDK's `HttpClient`,
  and JSON is handled by a hand-written recursive-descent parser
  (`JsonParser`/`JsonPath`/`JsonWriter`) with no third-party library.
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
- **Rich custom exceptions** - a `WorkflowException` hierarchy (now including
  `JsonException`) so any layer's failure can be caught generically or handled
  specifically.
- **173 JUnit 5 tests** covering the model, service and persistence layers,
  including the JSON engine, control-flow compiler/interpreter, a live
  in-process HTTP server test, salted-hash authentication, per-user workflow
  isolation and a timing-based concurrency test for the thread pool.
- **Two front ends over one core** - a Swing GUI (default) and a text
  console (`--console`), proving the domain layer is UI-agnostic.

## OOP concepts demonstrated

| Concept | Where |
|---|---|
| Abstraction | `Task` (abstract base), `WorkflowRepository` (interface), `FlowNode` (composite component) |
| Encapsulation | `Workflow` (private step list, bounds-safe mutators), `Condition` value object |
| Inheritance | All step classes extend `Task`; `SequenceNode`/`IfNode`/`LoopNode`/`TaskNode` extend `FlowNode` |
| Polymorphism | `WorkflowEngine` runs any `Task` via `execute(...)`; the interpreter walks any `FlowNode` |
| Template method | `Task.run(...)` wraps every step's `execute(...)` in uniform error handling |
| Composite pattern | Control flow is a tree of `FlowNode`s compiled from the flat list by `FlowParser` |
| Factory | `TaskFactory` builds concrete tasks from persisted fields |
| Programming to interfaces | service layer depends on `WorkflowRepository`, not the concrete SQLite/file class |
| Concurrency | `WorkflowExecutionService` runs workflows on a fixed `ExecutorService` thread pool; `CompletableFuture` streams results back to the EDT |
| Security | `AuthService` salts + hashes passwords with PBKDF2 and compares them in constant time |

## Project layout

```
src/main/java/com/flowforge
├── Main.java                  Entry point: login, then GUI (or --console)
├── exception/                 WorkflowException hierarchy (incl. JsonException, AuthenticationException)
├── model/                     Workflow, ExecutionContext, RunReport, StepResult, User
│   ├── task/                  Task base + steps + Condition + TaskType + TaskFactory
│   └── json/                  Dependency-free JSON parser, path resolver, writer
├── service/                   WorkflowManager, WorkflowEngine, AuthService,
│   │                          WorkflowExecutionService (thread pool), listener
│   └── flow/                  Composite control-flow tree + recursive interpreter
├── persistence/               WorkflowRepository + UserRepository + SQLite/file implementations
├── ui/                        ConsoleUI (text front end)
└── gui/                       FlowTheme, FlowForgeApp, LoginDialog, custom buttons/dialogs,
                               Cyberpunk fonts + neon button painting

lib/                           sqlite-jdbc driver
src/test/java/com/flowforge    JUnit 5 tests for model, json, service, flow, persistence
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

# Launch the GUI (opens the login/register screen first)
java -cp "out/main:lib/*" com.flowforge.Main

# Or the text console (prompts for login/register, then a text menu)
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
