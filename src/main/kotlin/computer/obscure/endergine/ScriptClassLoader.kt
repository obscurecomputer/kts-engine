package computer.obscure.endergine

class ScriptClassLoader(
    private val classFiles: Map<String, ByteArray>,
    private val config: SandboxConfig,
    private val hostLoader: ClassLoader = getSystemClassLoader()
) : ClassLoader(hostLoader) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        findLoadedClass(name)?.let { return it }

        val internalName = name.replace('.', '/') + ".class"
        classFiles[internalName]?.let { bytes ->
            return defineClass(name, bytes, 0, bytes.size).also {
                if (resolve) resolveClass(it)
            }
        }

        try {
            return Class.forName(name, false, null).also {
                if (resolve) resolveClass(it)
            }
        } catch (_: ClassNotFoundException) {}

        if (config.isAllowed(name)) {
            return hostLoader.loadClass(name).also {
                if (resolve) resolveClass(it)
            }
        }

        throw ClassNotFoundException("Sandboxed: $name")
    }
}