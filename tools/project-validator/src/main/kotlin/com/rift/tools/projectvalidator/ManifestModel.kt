package com.rift.tools.projectvalidator

import kotlinx.serialization.Serializable

@Serializable
data class RiftProjectManifest(
    val formatVersion: Int,
    val project: ProjectMetadata,
    val engine: EngineRequirement,
    val targets: List<String>,
    val entrypoint: String,
    val scenes: List<SceneDeclaration>,
    val modules: List<ModuleDeclaration> = emptyList(),
    val assetRoots: List<String> = listOf("assets")
)

@Serializable
data class ProjectMetadata(
    val id: String,
    val name: String
)

@Serializable
data class EngineRequirement(
    val version: String
)

@Serializable
data class SceneDeclaration(
    val id: String,
    val entrypoint: String
)

@Serializable
data class ModuleDeclaration(
    val id: String,
    val version: String,
    val dependencies: List<String> = emptyList(),
    val conflicts: List<String> = emptyList(),
    val capabilities: List<String> = emptyList()
)

@Serializable
enum class ManifestSeverity {
    ERROR,
    WARNING
}

@Serializable
data class ManifestIssue(
    val severity: ManifestSeverity,
    val code: String,
    val path: String,
    val message: String
)
