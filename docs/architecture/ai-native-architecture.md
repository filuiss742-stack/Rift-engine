# AI-Native Architecture

## Status

This document defines the architecture direction and acceptance criteria for making Rift Engine practical for AI-assisted game creation. It is a design contract, not a claim that the tooling described below is already implemented.

## Product principle

Rift Engine is an AI-native game constructor. AI agents and human developers use the same explicit project model, public APIs, validation rules, and change workflow. AI is a first-class authoring and orchestration client, not a privileged process that can bypass engine invariants.

The goal is not unrestricted mutation. The goal is broad creative freedom inside a system where changes are inspectable, testable, reversible, and explainable.

## 1. One project model

Every project should expose a machine-readable project description containing:

- project identity and format version;
- engine compatibility range;
- supported build targets;
- scenes, entry points, and shared game-state ownership;
- enabled modules and their versions;
- module dependencies, conflicts, and capabilities;
- asset roots and import settings;
- input maps and control profiles;
- build, test, validation, and packaging profiles.

The project model must be authoritative for tooling. Avoid requiring an agent to infer project structure from arbitrary folder names or load the entire repository into context. A schema and validator must reject unknown incompatible format versions and report precise paths for invalid fields.

This manifest and its validator are planned; they are not part of the current runtime yet.

## 2. Agent-facing capability surface

The engine should progressively expose a stable, documented set of operations. Prefer typed operations over unrestricted text edits when an operation has engine semantics.

Target operation groups:

| Group | Required operations |
|---|---|
| Project discovery | Describe project, list modules, list scenes, inspect targets and capabilities |
| Scene authoring | Create, inspect, update, duplicate, and remove scenes and scene objects |
| Gameplay composition | Add, configure, replace, disable, and remove modules |
| Assets | Import, inspect metadata, validate references, find usages, report missing assets |
| Runtime | Launch a selected scene, pause, resume, step, capture logs and diagnostics |
| Validation | Validate project, check dependencies, detect conflicts, run focused tests |
| Build | Build a named target and return structured result/artifact metadata |
| Change safety | Preview a change, apply a patch, inspect diff, revert a transaction |

Operations must have stable IDs, typed inputs, typed outputs, clear failure codes, and documented side effects. An agent must be able to discover available operations and their schemas without reading implementation source.

No agent-facing command server or editor bridge is implemented yet. Do not invent commands or claim these operations exist until they are shipped and tested.

## 3. Safe change transactions

A tool-assisted change should follow a predictable transaction:

1. Inspect the relevant project state and affected dependencies.
2. Propose a change plan with the files and engine objects it will touch.
3. Validate the plan against project/module constraints.
4. Apply the smallest patch.
5. Run focused validation and tests.
6. Present the diff, diagnostics, and test results.
7. Keep the change reversible.

Failed validation must not leave a half-applied project state. Destructive actions require explicit user approval. The agent must never silently rewrite unrelated assets, scenes, project settings, or source files.

## 4. Context designed for agents

Provide small, task-specific context bundles instead of asking agents to ingest the whole project. A context bundle should include:

- project summary and current format version;
- relevant scene/module/API descriptions;
- exact dependency and ownership information;
- applicable constraints and examples;
- current diagnostics relevant to the task;
- commands/checks required to verify the change.

Every public API should have concise machine-readable metadata and a human-readable explanation. Examples must be copyable and tested. Documentation must clearly separate implemented behavior, limitations, and planned work.

## 5. Composable systems, not genre locks

Gameplay modes are compositions of capabilities and systems, not rigid engine-level genre classes. A mode may select its own camera, controls, movement, physics world, renderer features, and combat rules while retaining shared game state where configured.

A project may transition from top-down exploration to platforming, first-person gameplay, racing, or a custom mode without duplicating player progression or replacing the entire runtime.

The module system must support explicit dependencies, conflicts, lifecycle, capability declarations, and safe replacement. Runtime module metadata validation is not proof of behavioral compatibility; tests and runtime diagnostics remain necessary.

## 6. Human-readable and AI-readable diagnostics

Every diagnostic should use a stable code and structured fields where possible:

- severity: info, warning, error, or fatal;
- code;
- concise message;
- affected resource, file, scene, module, or object;
- source location when available;
- confirmed cause versus hypothesis;
- actionable next step;
- related diagnostic IDs.

Diagnostics must not claim certainty when the engine only has a suspicion. Suggestions must not be auto-applied without approval. Fixes should be validated after application.

## 7. Runtime observability and iteration

AI-assisted creation needs a tight edit-run-observe loop. The engine should provide structured runtime events, logs, errors, scene state inspection, and reproducible test scenarios. Where a graphical preview is available, an agent should be able to launch a scene and obtain a screenshot or other inspectable output.

Runtime inspection must respect project permissions and avoid exposing secrets. Logs should be bounded and redact sensitive values by default.

## 8. Determinism and reproducibility

Where practical, simulation tests should support fixed seeds, controlled time steps, deterministic input playback, and reproducible asset import settings. Build and validation results should identify engine version, target, configuration, and relevant toolchain versions.

Do not promise bit-for-bit determinism across every GPU or platform. State the actual scope of each guarantee.

## 9. Extensibility without unsafe freedom

The engine should make extension easy but keep boundaries explicit:

- modules declare what they provide and require;
- ownership and cleanup are explicit;
- module changes are isolated and testable;
- incompatible combinations are rejected before activation where possible;
- optional module failure does not silently disable unrelated systems;
- external code is not treated as trusted merely because it is an engine module.

Arbitrary native code execution, downloaded binary plugins, and sandboxing require a separate security design. They are not implied by the current gameplay-module registry.

## 10. Implementation order

Build the AI-native workflow alongside real game capabilities, not as a detached tooling project:

1. **Project description and validation** — machine-readable project manifest, schema, precise validation.
2. **Action-based input and scene authoring** — usable game-facing APIs and concise examples.
3. **Asset catalog and reference validation** — stable asset IDs, metadata, missing-reference reports.
4. **Runtime diagnostics and observability** — structured events, reproducible test scenarios, scene inspection.
5. **Agent tool bridge** — discoverable typed operations for project, scenes, modules, assets, runtime, and builds.
6. **Transactional editing** — preview, apply, validate, diff, and rollback.
7. **Visual editor and module catalog** — human and AI share the same project model and operations.

Each step must deliver a usable vertical slice. Do not spend months building an abstract agent protocol before it can create and run a small playable game.

## 11. Definition of done for an AI-native feature

A feature is not AI-ready merely because an agent can edit its source code. It should provide:

- a discoverable API or operation;
- a concise explanation of purpose and limitations;
- a tested minimal example;
- structured validation and useful failure messages;
- explicit ownership and lifecycle;
- a focused test or reproducible verification command;
- compatibility information for Android and Windows where relevant.

The long-term acceptance test is simple: an agent with limited context can inspect a project, make a scoped gameplay change, run it, diagnose a failure, correct the change, and show the human exactly what changed without damaging unrelated work.
