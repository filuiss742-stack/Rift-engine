package com.rift.engine.demo

/** Authored world layout for the first, modern-world prologue chapter. */
data class PrologueRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun intersects(x: Float, y: Float, width: Float, height: Float): Boolean =
        x < right && x + width > left && y < bottom && y + height > top
}

data class PrologueObject(
    val id: String,
    val sprite: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val radius: Float = 78f,
    val interactable: Boolean = true
)

data class PrologueMap(
    val id: String,
    val background: String,
    val width: Float,
    val height: Float,
    val spawnX: Float,
    val spawnY: Float,
    val objects: List<PrologueObject>,
    val blockers: List<PrologueRect>
)

object ModernPrologueWorld {
    val apartment = PrologueMap(
        id = "apartment",
        background = "prologue/maps/apartment_room.png",
        width = 960f,
        height = 600f,
        spawnX = 462f,
        spawnY = 420f,
        objects = listOf(
            PrologueObject("bed", "bed", 86f, 72f, 128f, 112f, 92f),
            PrologueObject("desk", "desk", 692f, 70f, 112f, 88f, 90f),
            PrologueObject("fridge", "fridge", 838f, 346f, 72f, 112f, 88f),
            PrologueObject("sink", "sink", 690f, 372f, 92f, 78f, 82f),
            PrologueObject("phone", "phone", 418f, 292f, 56f, 88f, 76f),
            PrologueObject("exit", "door", 874f, 222f, 62f, 112f, 94f)
        ),
        blockers = listOf(
            PrologueRect(0f, 0f, 960f, 42f),
            PrologueRect(0f, 0f, 26f, 600f),
            PrologueRect(934f, 0f, 960f, 600f),
            PrologueRect(0f, 574f, 960f, 600f),
            PrologueRect(78f, 64f, 222f, 188f),
            PrologueRect(684f, 62f, 810f, 160f),
            PrologueRect(830f, 338f, 920f, 466f),
            PrologueRect(680f, 366f, 790f, 462f)
        )
    )

    val street = PrologueMap(
        id = "street",
        background = "prologue/maps/modern_street.png",
        width = 1280f,
        height = 720f,
        spawnX = 590f,
        spawnY = 414f,
        objects = listOf(
            PrologueObject("store", "storefront", 112f, 530f, 112f, 96f, 100f),
            PrologueObject("message", "phone", 584f, 314f, 56f, 88f, 86f),
            PrologueObject("bench", "bench", 808f, 548f, 112f, 72f, 92f),
            PrologueObject("tree_a", "tree", 22f, 286f, 96f, 112f, 0f, false),
            PrologueObject("tree_b", "tree", 1160f, 302f, 96f, 112f, 0f, false),
            PrologueObject("home", "door", 1084f, 520f, 62f, 112f, 98f)
        ),
        blockers = listOf(
            PrologueRect(0f, 0f, 1280f, 16f),
            PrologueRect(0f, 704f, 1280f, 720f),
            PrologueRect(0f, 0f, 16f, 720f),
            PrologueRect(1264f, 0f, 1280f, 720f),
            PrologueRect(36f, 28f, 276f, 228f),
            PrologueRect(332f, 28f, 614f, 228f),
            PrologueRect(692f, 28f, 938f, 228f),
            PrologueRect(992f, 28f, 1244f, 228f),
            PrologueRect(72f, 526f, 380f, 622f),
            PrologueRect(912f, 526f, 1200f, 622f)
        )
    )

    val fantasyWakeBackground = "prologue/maps/fantasy_wake.png"
}