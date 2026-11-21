# Branching Dialogue and Story Choices

Rift Engine now includes a small, platform-neutral dialogue runtime. It handles dialogue data and consequences; your game decides how the dialogue looks on screen.

## 1. Create a dialogue graph

A graph is a collection of uniquely named nodes. A node either points to the next line or offers choices.

```kotlin
val story = DialogueGraph(
    listOf(
        DialogueNode(
            id = "meeting",
            speaker = "Mira",
            text = "The bridge is guarded. What should we do?",
            choices = listOf(
                DialogueChoice(
                    id = "bribe",
                    text = "Offer 5 coins",
                    targetNodeId = "bribed",
                    conditions = listOf(StoryValueAtLeast("coins", 5)),
                    effects = listOf(
                        AddStoryValue("coins", -5),
                        SetStoryFlag("guard_bribed")
                    )
                ),
                DialogueChoice(
                    id = "sneak",
                    text = "Look for another way",
                    targetNodeId = "path_found"
                )
            )
        ),
        DialogueNode("bribed", "Mira", "The guard lets us pass."),
        DialogueNode("path_found", "Mira", "There is a narrow trail behind the rocks.")
    )
)
```

Every target node must exist. Node IDs and choice IDs must be unique. The graph validates these rules when created.

## 2. Start and display a conversation

```kotlin
val storyState = StoryState()
storyState.setValue("coins", 8)

val dialogue = DialogueSession(story, storyState)
dialogue.start("meeting")

val line = dialogue.currentNode
val visibleChoices = dialogue.availableChoices()
```

Your game UI reads speaker, text, and visible choices from the session and draws them with its chosen visual style. The current engine does not yet include a built-in dialogue window or visual-novel renderer.

## 3. Select a choice

```kotlin
dialogue.choose("bribe")
```

Only currently available choices can be selected. Selecting a choice applies its effects and moves the session to its target node. A locked or unknown choice returns false and changes nothing.

For a node with nextNodeId instead of choices, call:

```kotlin
dialogue.advance()
```

## 4. Make choices affect the whole game

Keep the same StoryState and share it with quests, characters, shops, endings, and other game systems.

- HasStoryFlag("guard_bribed") checks a story fact.
- StoryValueAtLeast("mira_trust", 3) checks a relationship or counter.
- SetStoryFlag("saved_village") records a permanent fact.
- AddStoryValue("mira_trust", 1) changes a relationship value.
- SetStoryValue("route", 2) sets a numeric route/state value.

A later dialogue can use those conditions to show different options. A quest or ending system can read the same state and react to earlier choices. This is the foundation for branching routes and consequential decisions; it does not automatically create game-specific quest or ending logic.

## 5. Important current limitations

- This is a dialogue/story logic runtime, not a finished visual-novel UI.
- No built-in portraits, backgrounds, text animation, sound cues, history log, auto-play, skip, or save-slot menu yet.
- Dialogue graphs are currently authored in Kotlin; no visual editor or JSON importer exists yet.
- StoryState exposes snapshots, but automatic save/load serialization is not included.
- A choice's effects happen immediately when it is selected.
- A scene or game decides when to start, pause, resume, or end a dialogue session.

Keep the narrative runtime generic. Put the actual story content and visual presentation in the game module, not in the engine core's platform adapters.
