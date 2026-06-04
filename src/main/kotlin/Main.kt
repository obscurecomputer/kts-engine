package me.znotchill

import java.io.File

suspend fun main() {
    val config = SandboxConfig.builder()
        .allow("net.minestom.server")
        .build()
    val runner = ScriptRunner(config)

    runner.run(
        "script.sandbox.kts",
        File("test.sandbox.kts").readText()
    )
}