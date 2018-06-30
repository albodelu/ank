package arrow.ank

import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.scope.VariantScope
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import java.io.File

class AnkAndroidPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val extension = AnkExtension()
        extensions.add("ank", extension)
        afterEvaluate {
            plugins.findPlugin(LibraryPlugin::class.java)?.variantScopes
                ?.forEach { scope ->
                    createAnkTask(scope, extension).dependsOn(listOf(createUnzipTask(scope)))
                }
                ?: error("Android library plugin not found")
        }
    }

    private fun Project.createAnkTask(scope: VariantScope, extension: AnkExtension) =
        task<JavaExec>(scope.variantData.getTaskName("runAnk", "")) {
            group = "ank"
            classpath = files(scope.classpath)
            main = "arrow.ank.main"
            args = ankArguments(
                source = extension.source ?: File("."), //scope.javaOutputDir,
                target = File(extension.target ?: File("."), "/${scope.fullVariantName}"),
                classpath = scope.classpath + files("$buildDir/unzipped/")
            )
        }

    private fun Project.createUnzipTask(scope: VariantScope) =
            task(scope.variantData.getTaskName("runUnzip", ""))
                    .dependsOn(scope.classpath.distinctBy { it ->  it.name }.filter{
                        it.toString().endsWith(".aar")
                    }.map {
                        createCopyTask(scope, it)
                    }
            )

    private fun Project.createCopyTask(scope: VariantScope, file: File) =
            task<Copy>(scope.variantData.getTaskName("runCopy", "${file.name}")) {
                from(zipTree(file))
                include("*.jar")
                rename("classes.jar", file.name.replace("aar", "jar"))
                into("$buildDir/unzipped")
            }
}

private val LibraryPlugin.variantScopes
    get() = variantManager.variantScopes

private inline fun <reified T : Task> Project.task(name: String, crossinline block: T.() -> Unit) =
    tasks.create(name, T::class.java) { it.block() }

private fun ankArguments(source: File, target: File, classpath: Set<File>): List<String> =
    listOf(source.absolutePath, target.absolutePath, *classpath.toPathArray())

private fun Set<File>.toPathArray(): Array<String> =
    map { it.toURI().toURL().toString() }.toTypedArray()

private val VariantScope.classpath
    get() = variantData.variantDependency.compileClasspath.resolve()

