package me.znotchill

import java.io.File

suspend fun main() {
    val config = SandboxConfig {
//        allow("net.minestom.")

        onError = { errors ->
            errors.forEach {
                println(it.blockedClass)
            }
        }
    }
    val runner = ScriptRunner(config)

    runner.run(
        "script.sandbox.kts",
        File("test.sandbox.kts").readText()
    )
}