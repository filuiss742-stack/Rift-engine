# Rift Project Manifest

The Rift project manifest is the machine-readable description of a game project. It is intended to give both human developers and AI agents one authoritative entry point instead of making them infer the project structure from folders and source files.

The current format is JSON and uses format version 1. The manifest validator lives in the tools/project-validator module.

## Example

See [the sample manifest](../examples/ai-game/rift.project.json).

## Fields

| Field | Required | Meaning |
|---|---|---|
| formatVersion | yes | Manifest format version. The current validator supports 1. |
| project.id | yes | Stable project ID: lowercase letter followed by lowercase letters, digits, underscore, or hyphen. |
| project.name | yes | Human-readable project name. |
| engine.version | yes | Non-empty engine version requirement. Compatibility range parsing is not implemented yet. |
| targets | yes | Unique supported build targets. Current supported values are android and windows. |
| entrypoint | yes | Fully qualified Kotlin class name implementing the game's entrypoint. |
| scenes | yes | At least one scene ID and fully qualified Kotlin class entrypoint. |
| modules | no | Enabled module IDs, versions, dependencies, conflicts, and capabilities. |
| assetRoots | no | Relative paths containing project assets. Defaults to assets. |

Unknown JSON fields are rejected by the current parser. This is intentional: misspelled or unsupported fields should not be silently ignored.

## Validate a manifest

From the Rift Engine repository root:

```shell
gradle :tools:project-validator:run --args="examples/ai-game/rift.project.json"
```

Exit codes:
- 0: no validation errors.
- 1: manifest read, parse, or validation error.
- 2: incorrect command-line usage.

The validator prints a JSON array of structured issues. Each issue includes severity, stable code, field path, and a concise message. AI tools should use the code and path rather than trying to parse prose.

## What validation guarantees

The validator currently checks:
- supported format version and build targets;
- project, scene, module, and entrypoint identifiers;
- duplicate scene/module/target declarations;
- missing module dependencies, contradictory relationships, and dependency cycles;
- unsafe asset-root paths that could escape the project directory.

The validator does not yet prove that Kotlin entrypoint classes compile, that every referenced asset exists, that a module is behaviorally compatible, or that Android/Windows packaging will succeed. Those checks belong to later build, asset, and runtime validation stages. Do not treat a valid manifest as proof that the game is buildable.
