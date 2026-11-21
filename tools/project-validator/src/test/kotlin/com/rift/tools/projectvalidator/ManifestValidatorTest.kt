package com.rift.tools.projectvalidator

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestValidatorTest {
    @Test
    fun acceptsValidAndroidAndWindowsProject() {
        val issues = ManifestValidator.validate(validManifest())

        assertTrue(issues.isEmpty())
    }

    @Test
    fun reportsUnsupportedTargetAndInvalidEntrypoint() {
        val manifest = validManifest().copy(
            targets = listOf("android", "linux"),
            entrypoint = "not a class name"
        )

        val issues = ManifestValidator.validate(manifest)

        assertTrue(issues.any { it.code == "RIFT_TARGET_UNSUPPORTED" && it.path == "targets[1]" })
        assertTrue(issues.any { it.code == "RIFT_ENTRYPOINT_INVALID" })
    }

    @Test
    fun detectsMissingDependenciesAndDependencyCycles() {
        val manifest = validManifest().copy(
            modules = listOf(
                ModuleDeclaration(
                    id = "combat",
                    version = "1.0.0",
                    dependencies = listOf("movement")
                ),
                ModuleDeclaration(
                    id = "movement",
                    version = "1.0.0",
                    dependencies = listOf("combat", "missing")
                )
            )
        )

        val issues = ManifestValidator.validate(manifest)

        assertTrue(issues.any { it.code == "RIFT_MODULE_DEPENDENCY_MISSING" })
        assertTrue(issues.any { it.code == "RIFT_MODULE_DEPENDENCY_CYCLE" })
    }

    @Test
    fun rejectsAssetRootsThatEscapeTheProject() {
        val manifest = validManifest().copy(
            assetRoots = listOf("assets", "../shared-assets", "C:\\outside")
        )

        val issues = ManifestValidator.validate(manifest)

        assertEquals(2, issues.count { it.code == "RIFT_ASSET_ROOT_UNSAFE" })
    }


    @Test
    fun validatesEntrypointSourcesAndExistingAssetRoots() {
        val root = Files.createTempDirectory("rift-project").toFile()
        val sourceDir = File(root, "src/main/kotlin/com/example/game")
        val assetsDir = File(root, "assets")
        assertTrue(sourceDir.mkdirs())
        assertTrue(assetsDir.mkdirs())
        File(sourceDir, "SampleGame.kt").writeText("package com.example.game")
        File(sourceDir, "WorldScene.kt").writeText("package com.example.game")

        val issues = ManifestValidator.validate(validManifest(), root)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun reportsMissingEntrypointSourcesAndAssetRoots() {
        val root = Files.createTempDirectory("rift-project-missing").toFile()

        val issues = ManifestValidator.validate(validManifest(), root)

        assertTrue(issues.any { it.code == "RIFT_ENTRYPOINT_SOURCE_MISSING" })
        assertTrue(issues.any { it.code == "RIFT_SCENE_SOURCE_MISSING" })
        assertTrue(issues.any { it.code == "RIFT_ASSET_ROOT_MISSING" })
    }

    @Test
    fun reportsDuplicateSceneIds() {
        val manifest = validManifest().copy(
            scenes = listOf(
                SceneDeclaration("world", "com.example.game.WorldScene"),
                SceneDeclaration("world", "com.example.game.OtherScene")
            )
        )

        val issues = ManifestValidator.validate(manifest)

        assertTrue(issues.any { it.code == "RIFT_SCENE_ID_DUPLICATE" })
        assertFalse(issues.any { it.code == "RIFT_SCENE_ENTRYPOINT_INVALID" })
    }

    private fun validManifest() = RiftProjectManifest(
        formatVersion = 1,
        project = ProjectMetadata(
            id = "sample_game",
            name = "Sample Game"
        ),
        engine = EngineRequirement(version = "0.1.0"),
        targets = listOf("android", "windows"),
        entrypoint = "com.example.game.SampleGame",
        scenes = listOf(
            SceneDeclaration(
                id = "world",
                entrypoint = "com.example.game.WorldScene"
            )
        ),
        modules = listOf(
            ModuleDeclaration(
                id = "movement",
                version = "1.0.0",
                capabilities = listOf("movement.top-down")
            )
        ),
        assetRoots = listOf("assets")
    )
}
