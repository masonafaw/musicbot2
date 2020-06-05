package fredboat.config.property

class RatelimitConfig(
        @Deprecated("TODO: Remove this later")
        var balancingBlock: String = "",
        var ipBlocks: List<String> = emptyList(),
        var excludedIps: List<String> = emptyList(),
        var strategy: String = "RotateOnBan",
        var searchTriggersFail: Boolean = true
)