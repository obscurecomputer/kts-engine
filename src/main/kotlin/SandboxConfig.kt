package me.znotchill

class SandboxConfig private constructor(
    val allowed: Set<String>,
    val blocked: Set<String>,
    val blockedMethodsByClass: Map<String, Set<String>>,
) {
    fun isAllowed(className: String): Boolean {
        if (isEngineClass(className)) return true

        if (BASELINE_BLOCKED.any { className.startsWith(it) })
            return false
        if (blocked.any { className.startsWith(it) })
            return false

        if (BASELINE_ALLOWED.any { className.startsWith(it) })
            return true
        if (allowed.any { className.startsWith(it) })
            return true

        return false
    }

    fun blockedMethods(className: String): Set<String>? {
        val baseline = BASELINE_BLOCKED_METHODS[className]
        val custom = blockedMethodsByClass[className]
        return when {
            baseline != null && custom != null -> baseline + custom
            else -> baseline ?: custom
        }
    }

    companion object {
        val BASELINE_ALLOWED = setOf(
            "java.lang.Object",
            "kotlin.",
            "kotlinx.",
            "java.lang.String",
            "java.lang.Number",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.Math",
            "java.lang.Iterable",
            "java.lang.Comparable",
            "java.lang.Enum",
            "java.util.",
            "java.math.",
            "java.time.",
            "java.io.PrintStream",
            "java.io.PrintWriter",
            "java.io.Serializable",
        )

        val BASELINE_BLOCKED = setOf(
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.ClassLoader",
            "java.lang.Thread",
            "java.lang.reflect.",
            "java.lang.invoke.",
            "sun.",
            "com.sun.",
            "jdk.internal.",
            "java.nio.file.",
            "java.net.",
        )

        val BASELINE_BLOCKED_METHODS = mapOf(
            "java.lang.System" to setOf(
                "exit", "load", "loadLibrary",
                "setSecurityManager", "setProperty",
                "setProperties", "getenv", "gc"
            ),
            "java.lang.Runtime" to setOf("*"),
            "java.lang.ProcessBuilder" to setOf("*"),
        )

        internal val ENGINE_PACKAGE = SandboxScript::class.java.packageName + "."

        fun isEngineClass(className: String) = className.startsWith(ENGINE_PACKAGE)

        fun builder() = Builder()
    }

    class Builder {
        private val allowed = mutableSetOf<String>()
        private val blocked = mutableSetOf<String>()
        private val blockedMethodsByClass = mutableMapOf<String, MutableSet<String>>()

        fun allow(vararg prefixes: String) = apply { allowed.addAll(prefixes) }
        fun block(vararg names: String) = apply { blocked.addAll(names) }
        fun blockMethods(className: String, vararg methods: String) = apply {
            blockedMethodsByClass.getOrPut(className) { mutableSetOf() }.addAll(methods)
        }

        fun build() = SandboxConfig(
            allowed.toSet(),
            blocked.toSet(),
            blockedMethodsByClass.mapValues { it.value.toSet() }
        )
    }
}