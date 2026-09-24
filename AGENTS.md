# AGENTS.md

## Project overview

This repository is a Java workflow engine focused on YAML-defined playbooks. The core flow is:

1. Parse YAML into a `Playbook` via `YamlReader`
2. Register built-in or custom task classes with `PlaybookManager.register(...)`
3. Execute the playbook through `PlaybookManager.run(...)`
4. Tasks are executed as `Work` implementations under the `org.jeasy.flows.work` package

Primary entry points:
- [README.md](README.md)
- [pom.xml](pom.xml)
- [src/main/java/org/jeasy/flows/playbook/PlaybookManager.java](src/main/java/org/jeasy/flows/playbook/PlaybookManager.java)
- [src/main/java/org/jeasy/flows/reader/YamlReader.java](src/main/java/org/jeasy/flows/reader/YamlReader.java)

## Build and test workflow

Use Maven from the repository root:

- `mvn test` — run the full test suite
- `mvn -Dtest=org.jeasy.flows.playbook.PlaybookManagerTest test` — run a focused test class
- `mvn -q test` — quiet local validation when iterating quickly

This project targets Java 17 and uses JUnit 5 for tests.

## Repository conventions

- Keep task implementations under `src/main/java/org/jeasy/flows/work` and prefer the `Work` interface contract.
- Favor YAML-driven behavior and playbook-level orchestration over code-only workflow setup.
- When adding a task, also register it in `PlaybookManager.register(...)` if it is intended to be usable by ID in playbooks.
- Tests are the main guardrail for parsing and execution behavior; add or update tests alongside functional changes.
- The project is intentionally configuration-first: playbooks encode workflow structure, variables, conditional logic, repeats, and parallel blocks.

## Practical patterns

- `PlaybookManager` resolves task IDs from the registered task map; unknown IDs cause execution to fail.
- Built-in task IDs such as `log`, `noop`, `set`, `when`, and `evaluate` are pre-registered when possible.
- YAML examples in the README are the best reference for accepted syntax and task composition.
- Parallel and block steps are structured as nested `Task` entries with task lists, not as ad hoc Java object graphs.

## When making changes

- Prefer minimal, targeted edits that stay aligned with the playbook model and YAML parser.
- Reproduce/extend behavior with a failing test before fixing parser or execution logic.
- Check the relevant task and playbook tests in `src/test/java/org/jeasy/flows` before broad refactors.

## Helpful files

- [src/test/java/org/jeasy/flows/playbook/PlaybookManagerTest.java](src/test/java/org/jeasy/flows/playbook/PlaybookManagerTest.java)
- [src/test/java/org/jeasy/flows/reader/YamlReaderTest.java](src/test/java/org/jeasy/flows/reader/YamlReaderTest.java)
- [src/main/java/org/jeasy/flows/work](src/main/java/org/jeasy/flows/work)
