package com.rift.tools.projectvalidator

/**
 * Validates manifest structure and declared relationships.
 *
 * This validator checks metadata only. It cannot prove that referenced Kotlin
 * classes compile, assets exist in every packaging configuration, or modules
 * are behaviorally compatible at runtime.
 */
object ManifestValidator {
    private val supportedTargets = setOf("android", "windows")
    private val projectIdPattern = Regex("^[a-z][a-z0-9_-]{0,62}$")
    private val moduleIdPattern = Regex("^[a-z][a-z0-9._-]{0,127}$")
    private val classNamePattern = Regex("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*$")

    fun validate(
        manifest: RiftProjectManifest,
        projectRoot: File? = null
    ): List<ManifestIssue> {
        val issues = mutableListOf<ManifestIssue>()

        fun error(code: String, path: String, message: String) {
            issues += ManifestIssue(ManifestSeverity.ERROR, code, path, message)
        }

        fun warning(code: String, path: String, message: String) {
            issues += ManifestIssue(ManifestSeverity.WARNING, code, path, message)
        }

        if (manifest.modules.size > 256) {
            error("RIFT_MODULE_LIMIT_EXCEEDED", "modules", "A project may declare at most 256 modules.")
        }
        if (manifest.scenes.size > 512) {
            error("RIFT_SCENE_LIMIT_EXCEEDED", "scenes", "A project may declare at most 512 scenes.")
        }
        if (manifest.assetRoots.size > 64) {
            error("RIFT_ASSET_ROOT_LIMIT_EXCEEDED", "assetRoots", "A project may declare at most 64 asset roots.")
        }

        if (manifest.formatVersion != 1) {
            error(
                "RIFT_MANIFEST_VERSION_UNSUPPORTED",
                "formatVersion",
                "Manifest format version " + manifest.formatVersion + " is not supported. Supported version: 1."
            )
        }

        if (!projectIdPattern.matches(manifest.project.id)) {
            error(
                "RIFT_PROJECT_ID_INVALID",
                "project.id",
                "Project ID must start with a lowercase letter and contain only lowercase letters, digits, underscores, or hyphens."
            )
        }
        if (manifest.project.name.isBlank()) {
            error("RIFT_PROJECT_NAME_EMPTY", "project.name", "Project name must not be blank.")
        }
        if (manifest.engine.version.isBlank()) {
            error("RIFT_ENGINE_VERSION_EMPTY", "engine.version", "Engine version requirement must not be blank.")
        }

        if (manifest.targets.isEmpty()) {
            error("RIFT_TARGETS_EMPTY", "targets", "At least one build target must be declared.")
        }
        manifest.targets.forEachIndexed { index, target ->
            if (target !in supportedTargets) {
                error(
                    "RIFT_TARGET_UNSUPPORTED",
                    "targets[" + index + "]",
                    "Unsupported target '" + target + "'. Supported targets: " + supportedTargets.sorted().joinToString() + "."
                )
            }
        }
        duplicateValues(manifest.targets).forEach { target ->
            error("RIFT_TARGET_DUPLICATE", "targets", "Target '" + target + "' is declared more than once.")
        }

        if (!classNamePattern.matches(manifest.entrypoint)) {
            error(
                "RIFT_ENTRYPOINT_INVALID",
                "entrypoint",
                "Entrypoint must be a fully qualified Kotlin class name."
            )
        } else if (projectRoot != null && !sourceExists(projectRoot, manifest.entrypoint)) {
            error(
                "RIFT_ENTRYPOINT_SOURCE_MISSING",
                "entrypoint",
                "No Kotlin/Java source file was found for entrypoint '" + manifest.entrypoint + "'."
            )
        }

        if (manifest.scenes.isEmpty()) {
            error("RIFT_SCENES_EMPTY", "scenes", "At least one scene must be declared.")
        }
        manifest.scenes.forEachIndexed { index, scene ->
            if (!moduleIdPattern.matches(scene.id)) {
                error(
                    "RIFT_SCENE_ID_INVALID",
                    "scenes[" + index + "].id",
                    "Scene ID must start with a lowercase letter and contain only lowercase letters, digits, dots, underscores, or hyphens."
                )
            }
            if (!classNamePattern.matches(scene.entrypoint)) {
                error(
                    "RIFT_SCENE_ENTRYPOINT_INVALID",
                    "scenes[" + index + "].entrypoint",
                    "Scene entrypoint must be a fully qualified Kotlin class name."
                )
            } else if (projectRoot != null && !sourceExists(projectRoot, scene.entrypoint)) {
                error(
                    "RIFT_SCENE_SOURCE_MISSING",
                    "scenes[" + index + "].entrypoint",
                    "No Kotlin/Java source file was found for scene '" + scene.entrypoint + "'."
                )
            }
        }
        duplicateValues(manifest.scenes.map { it.id }).forEach { id ->
            error("RIFT_SCENE_ID_DUPLICATE", "scenes", "Scene ID '" + id + "' is declared more than once.")
        }

        val modulesById = manifest.modules.associateBy { it.id }
        manifest.modules.forEachIndexed { index, module ->
            val basePath = "modules[" + index + "]"
            if (!moduleIdPattern.matches(module.id)) {
                error(
                    "RIFT_MODULE_ID_INVALID",
                    basePath + ".id",
                    "Module ID must start with a lowercase letter and contain only lowercase letters, digits, dots, underscores, or hyphens."
                )
            }
            if (module.version.isBlank()) {
                error("RIFT_MODULE_VERSION_EMPTY", basePath + ".version", "Module version must not be blank.")
            }
            if (module.id in module.dependencies) {
                error("RIFT_MODULE_SELF_DEPENDENCY", basePath + ".dependencies", "A module cannot depend on itself.")
            }
            if (module.id in module.conflicts) {
                error("RIFT_MODULE_SELF_CONFLICT", basePath + ".conflicts", "A module cannot conflict with itself.")
            }
            if (module.dependencies.any { it.isBlank() } ||
                module.conflicts.any { it.isBlank() } ||
                module.capabilities.any { it.isBlank() }
            ) {
                error(
                    "RIFT_MODULE_METADATA_EMPTY",
                    basePath,
                    "Dependency, conflict, and capability entries must not be blank."
                )
            }
            val overlap = module.dependencies.toSet().intersect(module.conflicts.toSet())
            if (overlap.isNotEmpty()) {
                error(
                    "RIFT_MODULE_RELATIONSHIP_CONTRADICTORY",
                    basePath,
                    "A module cannot both require and conflict with: " + overlap.sorted().joinToString() + "."
                )
            }
            duplicateValues(module.dependencies).forEach {
                error("RIFT_MODULE_DEPENDENCY_DUPLICATE", basePath + ".dependencies", "Dependency '" + it + "' is repeated.")
            }
            duplicateValues(module.conflicts).forEach {
                error("RIFT_MODULE_CONFLICT_DUPLICATE", basePath + ".conflicts", "Conflict '" + it + "' is repeated.")
            }
            duplicateValues(module.capabilities).forEach {
                warning("RIFT_MODULE_CAPABILITY_DUPLICATE", basePath + ".capabilities", "Capability '" + it + "' is repeated.")
            }
        }
        duplicateValues(manifest.modules.map { it.id }).forEach { id ->
            error("RIFT_MODULE_ID_DUPLICATE", "modules", "Module ID '" + id + "' is declared more than once.")
        }

        manifest.modules.forEachIndexed { index, module ->
            module.dependencies.forEach { dependency ->
                if (dependency !in modulesById) {
                    error(
                        "RIFT_MODULE_DEPENDENCY_MISSING",
                        "modules[" + index + "].dependencies",
                        "Module '" + module.id + "' requires undeclared module '" + dependency + "'."
                    )
                }
                if (module.conflicts.contains(dependency)) {
                    error(
                        "RIFT_MODULE_RELATIONSHIP_CONTRADICTORY",
                        "modules[" + index + "]",
                        "Module '" + module.id + "' both requires and conflicts with '" + dependency + "'."
                    )
                }
            }
            module.conflicts.forEach { conflict ->
                val other = modulesById[conflict]
                if (other != null && module.id in other.dependencies) {
                    error(
                        "RIFT_MODULE_CONFLICTS_WITH_DEPENDENT",
                        "modules[" + index + "].conflicts",
                        "Module '" + module.id + "' conflicts with '" + conflict + "', which declares it as a dependency."
                    )
                }
            }
        }

        detectDependencyCycles(manifest.modules).forEach { cycle ->
            error(
                "RIFT_MODULE_DEPENDENCY_CYCLE",
                "modules",
                "Module dependency cycle detected: " + cycle.joinToString(" -> ") + "."
            )
        }

        if (manifest.assetRoots.isEmpty()) {
            warning("RIFT_ASSET_ROOTS_EMPTY", "assetRoots", "No asset roots are declared.")
        }
        manifest.assetRoots.forEachIndexed { index, root ->
            val normalized = root.replace('\\', '/')
            val segments = normalized.split('/')
            val safe = root.isNotBlank() &&
                !normalized.startsWith("/") &&
                !Regex("^[A-Za-z]:").containsMatchIn(normalized) &&
                segments.none { it == ".." || it.isBlank() }
            if (!safe) {
                error(
                    "RIFT_ASSET_ROOT_UNSAFE",
                    "assetRoots[" + index + "]",
                    "Asset root must be a non-empty relative path and must not traverse outside the project."
                )
            } else if (projectRoot != null) {
                val canonicalProject = projectRoot.canonicalFile
                val candidate = File(canonicalProject, normalized).canonicalFile
                val rootPrefix = canonicalProject.path + File.separator
                if (!(candidate.path.startsWith(rootPrefix) || candidate == canonicalProject)) {
                    error(
                        "RIFT_ASSET_ROOT_UNSAFE",
                        "assetRoots[" + index + "]",
                        "Asset root resolves outside the project directory."
                    )
                } else if (!candidate.isDirectory) {
                    error(
                        "RIFT_ASSET_ROOT_MISSING",
                        "assetRoots[" + index + "]",
                        "Asset root directory does not exist: " + normalized + "."
                    )
                }
            }
        }
        duplicateValues(manifest.assetRoots).forEach { root ->
            warning("RIFT_ASSET_ROOT_DUPLICATE", "assetRoots", "Asset root '" + root + "' is repeated.")
        }

        return issues
    }

    private fun sourceExists(projectRoot: File, className: String): Boolean {
        val relativeSource = className.replace('.', File.separatorChar) + ".kt"
        val javaSource = className.replace('.', File.separatorChar) + ".java"
        val canonicalRoot = projectRoot.canonicalFile
        val rootPrefix = canonicalRoot.path + File.separator
        return listOf(
            File(canonicalRoot, "src/main/kotlin" + File.separator + relativeSource),
            File(canonicalRoot, "src/main/java" + File.separator + javaSource)
        ).any { source ->
            val canonicalSource = source.canonicalFile
            canonicalSource.path.startsWith(rootPrefix) && canonicalSource.isFile
        }
    }

    private fun duplicateValues(values: List<String>): Set<String> {
        return values.groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys
    }

    private fun detectDependencyCycles(modules: List<ModuleDeclaration>): List<List<String>> {
        val dependencies = modules.associate { it.id to it.dependencies }
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val reported = mutableSetOf<String>()
        val cycles = mutableListOf<List<String>>()
        val stack = mutableListOf<String>()

        fun visit(id: String) {
            if (id in visited) return
            if (!visiting.add(id)) {
                val start = stack.indexOf(id).coerceAtLeast(0)
                val cycle = stack.subList(start, stack.size).toList() + id
                val key = cycle.dropLast(1).sorted().joinToString("|")
                if (reported.add(key)) cycles += cycle
                return
            }

            stack.add(id)
            dependencies[id].orEmpty().forEach { dependency ->
                if (dependency in dependencies) visit(dependency)
            }
            stack.removeAt(stack.lastIndex)
            visiting.remove(id)
            visited.add(id)
        }

        dependencies.keys.forEach(::visit)
        return cycles
    }
}
