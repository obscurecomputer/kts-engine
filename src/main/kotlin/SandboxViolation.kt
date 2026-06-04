package me.znotchill

data class SandboxViolation(
    val className: String,
    val method: String,
    val blockedClass: String
)
