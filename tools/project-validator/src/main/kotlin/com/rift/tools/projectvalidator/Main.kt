package com.rift.tools.projectvalidator

import java.io.File
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.system.exitProcess

private val outputJson = Json {
    prettyPrint = true
}

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: rift-project-validator <path-to-rift.project.json>")
        exitProcess(2)
    }

    val manifestFile = File(args[0])
    if (!manifestFile.isFile) {
        printIssues(
            listOf(
                ManifestIssue(
                    severity = ManifestSeverity.ERROR,
                    code = "RIFT_MANIFEST_FILE_NOT_FOUND",
                    path = "$",
                    message = "Manifest file does not exist: " + manifestFile.path
                )
            )
        )
        exitProcess(1)
    }

    val manifest = try {
        outputJson.decodeFromString<RiftProjectManifest>(manifestFile.readText())
    } catch (exception: SerializationException) {
        printIssues(
            listOf(
                ManifestIssue(
                    severity = ManifestSeverity.ERROR,
                    code = "RIFT_MANIFEST_PARSE_FAILED",
                    path = "$",
                    message = exception.message ?: "Manifest JSON could not be parsed."
                )
            )
        )
        exitProcess(1)
    } catch (exception: Exception) {
        printIssues(
            listOf(
                ManifestIssue(
                    severity = ManifestSeverity.ERROR,
                    code = "RIFT_MANIFEST_READ_FAILED",
                    path = "$",
                    message = exception.message ?: "Manifest file could not be read."
                )
            )
        )
        exitProcess(1)
    }

    val issues = ManifestValidator.validate(manifest)
    printIssues(issues)

    if (issues.any { it.severity == ManifestSeverity.ERROR }) {
        exitProcess(1)
    }
}

private fun printIssues(issues: List<ManifestIssue>) {
    println(outputJson.encodeToString(issues))
}
