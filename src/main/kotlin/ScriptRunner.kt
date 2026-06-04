package me.znotchill

import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleInMemory
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.compilerOptions

@KotlinScript(fileExtension = "sandbox.kts")
abstract class SandboxScript

class ScriptRunner(
    val config: SandboxConfig = SandboxConfig.builder()
        .build()
) {
    val compilationConfig = ScriptCompilationConfiguration {
        baseClass(SandboxScript::class)
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
        compilerOptions("-jvm-target", Runtime.version().feature().toString())
    }

    val compiler = JvmScriptCompiler()
    suspend fun run(name: String, source: String): ResultWithDiagnostics<*> {
        val scriptSource = source.toScriptSource(name)
        val result = compiler(scriptSource, compilationConfig)

        when (result) {
            is ResultWithDiagnostics.Success -> {
                val compiled = result.value as KJvmCompiledScript
                val module = compiled.getCompiledModule() as KJvmCompiledModuleInMemory
                val classFiles = module.compilerOutputFiles.filter { (name, _) -> name.endsWith(".class") }

                val inspector = BytecodeInspector(config)
                val violations = inspector.inspect(classFiles)

                if (violations.isNotEmpty()) {
                    violations.forEach { println("  ${it.className}#${it.method}: ${it.blockedClass}") }
                } else {
                    val loader = ScriptClassLoader(classFiles, config)
                    val scriptClass = loader.loadClass("Script_sandbox")

                    scriptClass.getDeclaredConstructor().newInstance()
                }
            }
            is ResultWithDiagnostics.Failure -> {
                result.reports.forEach { println(it) }
            }
        }

        return result
    }

    companion object {
        internal val ENGINE_PACKAGE = SandboxScript::class.java.packageName + "."

        fun isEngineClass(className: String) = className.startsWith(ENGINE_PACKAGE)
    }
}