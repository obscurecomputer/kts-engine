package me.znotchill.endergine

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Type

class BytecodeInspector(
    private val config: SandboxConfig
) {
    fun inspect(classFiles: Map<String, ByteArray>): List<SandboxViolation> {
        val scriptClasses = classFiles.keys
            .filter { it.endsWith(".class") }
            .map { it.removeSuffix(".class") }
            .toSet()

        return classFiles
            .filter { (name, _) -> name.endsWith(".class") }
            .flatMap { (_, bytes) -> inspectClass(bytes, scriptClasses) }
    }

    private fun inspectClass(bytes: ByteArray, scriptClasses: Set<String>): List<SandboxViolation> {
        val violations = mutableListOf<SandboxViolation>()
        val reader = ClassReader(bytes)
        reader.accept(ViolationVisitor(violations, scriptClasses), 0)
        return violations
    }

    private inner class ViolationVisitor(
        private val violations: MutableList<SandboxViolation>,
        private val scriptClasses: Set<String>,
    ) : ClassVisitor(ASM9) {

        private var currentClass = ""

        override fun visit(
            version: Int, access: Int, name: String,
            signature: String?, superName: String?, interfaces: Array<out String>?
        ) {
            currentClass = name
        }

        override fun visitMethod(
            access: Int, name: String, descriptor: String,
            signature: String?, exceptions: Array<out String>?
        ): MethodVisitor {
            return InstructionVisitor(currentClass, name, violations, scriptClasses)
        }
    }

    private inner class InstructionVisitor(
        private val currentClass: String,
        private val currentMethod: String,
        private val violations: MutableList<SandboxViolation>,
        private val scriptClasses: Set<String>,
    ) : MethodVisitor(ASM9) {

        override fun visitMethodInsn(
            opcode: Int, owner: String, name: String,
            descriptor: String, isInterface: Boolean
        ) {
            val dotName = owner.replace('/', '.')
            val blockedMethods = config.blockedMethods(dotName)
            if (blockedMethods != null) {
                if ("*" in blockedMethods || name in blockedMethods) {
                    violations.add(SandboxViolation(currentClass, currentMethod, "$dotName.$name"))
                }
                return
            }
            check(dotName)
        }

        override fun visitFieldInsn(
            opcode: Int, owner: String, name: String, descriptor: String
        ) {
            val dotName = owner.replace('/', '.')
            if (config.blockedMethods(dotName) != null) return
            check(dotName)
        }

        override fun visitLdcInsn(value: Any?) {
            if (value is Type) {
                val dotName = value.internalName.replace('/', '.')
                if (dotName == currentClass.replace('/', '.')) return
                check(value.internalName)
            }
        }

        override fun visitTypeInsn(opcode: Int, type: String) {
            if (type == currentClass || type.startsWith("$currentClass\$")) return
            if (opcode == NEW) check(type)
        }

        private fun check(internalName: String) {
            if (internalName in scriptClasses) return
            val dotName = internalName.replace('/', '.')
            if (!config.isAllowed(dotName)) {
                violations.add(
                    SandboxViolation(currentClass, currentMethod, dotName)
                )
            }
        }
    }
}