package computer.obscure.endergine

import kotlin.collections.component1
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleInMemory
import kotlin.script.experimental.jvm.impl.KJvmCompiledScript
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.JvmScriptCompiler

class ScriptRunner(
    val config: SandboxConfig = SandboxConfig {},
    val compilationConfig: ScriptCompilationConfiguration = ScriptCompilationConfiguration {
        baseClass(SandboxScript::class)

        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }

        compilerOptions(
            "-jvm-target",
            Runtime.version().feature().toString()
        )
    }
) {
    val compiler = JvmScriptCompiler()

    class CompiledScript(
        val classFiles: Map<String, ByteArray>,
        val scriptClassName: String
    )

    suspend fun compile(
        name: String,
        source: String
    ): ResultWithDiagnostics<CompiledScript> {
        val scriptSource = source.toScriptSource(name)
        val result = compiler(scriptSource, compilationConfig)

        return when (result) {
            is ResultWithDiagnostics.Success -> {
                val compiled = result.value as KJvmCompiledScript
                val module = compiled.getCompiledModule() as KJvmCompiledModuleInMemory

                val classFiles = module.compilerOutputFiles
                    .filter { (name, _) -> name.endsWith(".class") }

                val inspector = BytecodeInspector(config)
                val violations = inspector.inspect(classFiles)

                if (violations.isNotEmpty()) {
                    config.onError(violations)
                    return ResultWithDiagnostics.Failure(
                        ScriptDiagnostic(
                            code = 1,
                            message = "Script rejected by sandbox: ${violations.size} violation(s)",
                            severity = ScriptDiagnostic.Severity.ERROR,
                            locationWithId = null
                        )
                    )
                }

                val scriptClassName = if (config.extension != null) {
                    classFiles.keys.firstOrNull {
                        it.endsWith("_${config.extension}.class")
                    }
                } else {
                    classFiles.keys.firstOrNull()
                }

                if (scriptClassName == null) {
                    return ResultWithDiagnostics.Failure(
                        ScriptDiagnostic(
                            code = 2,
                            message = "No script class found in compiled output",
                            severity = ScriptDiagnostic.Severity.ERROR,
                            locationWithId = null
                        )
                    )
                }

                ResultWithDiagnostics.Success(
                    CompiledScript(
                        classFiles = classFiles,
                        scriptClassName = scriptClassName.removeSuffix(".class")
                    ),
                    result.reports
                )
            }

            is ResultWithDiagnostics.Failure -> {
                result.reports.forEach { println(it) }
                result
            }
        }
    }

    fun execute(compiled: CompiledScript) {
        val loader = ScriptClassLoader(
            compiled.classFiles,
            config,
            config.classLoader
        )

        val scriptClass = loader.loadClass(compiled.scriptClassName)

        println("SCRIPT CLASS: $scriptClass")

        scriptClass.declaredConstructors.forEach {
            println("CONSTRUCTOR: $it")
        }

        scriptClass.declaredMethods.forEach {
            println("METHOD: $it")
        }

        scriptClass
            .getDeclaredConstructor()
            .newInstance()
    }

    companion object {
        internal val ENGINE_PACKAGE =
            SandboxScript::class.java.packageName + "."

        fun isEngineClass(className: String) =
            className.startsWith(ENGINE_PACKAGE)
    }
}
