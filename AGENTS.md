# Instructions for AI Agents

Read `AI_RULES.md` before changing the repository. Rift Engine targets Android and Windows.

- Keep `:engine` pure Kotlin/JVM: no Android, AWT or Swing imports.
- Put platform integrations in `:platform-android` or `:platform-desktop`.
- Keep game-specific content in game/demo modules.
- Inspect relevant source and callers before editing.
- Prefer composition and explicit APIs; avoid god classes, hidden state and circular dependencies.
- Use Rift Diagnostics honestly; distinguish confirmed errors, warnings and hypotheses.
- Every new or materially upgraded plugin needs complete docs in `docs/plugins/<plugin-id>.md`.
- Update docs/examples, run available tests/builds and report only verified results.
- Do not add Linux/macOS support claims unless the project owner changes the target policy.
