package computer.obscure.endergine

data class SandboxViolation(
    val className: String,
    val method: String,
    val blockedClass: String
)
