package fredboat.perms

val NO_PERMISSIONS = PermissionSet(0L)

enum class Permission(offset: Int, val uiName: String) : IPermissionSet {

    CREATE_INSTANT_INVITE(0, "Create Instant Invite"),
    KICK_MEMBERS(1, "Kick Members"),
    BAN_MEMBERS(2, "Ban Members"),
    ADMINISTRATOR(3, "Administrator"),
    MANAGE_CHANNEL(4, "Manage Channels"),
    MANAGE_SERVER(5, "Manage Server"),
    MESSAGE_ADD_REACTION(6, "Add Reactions"),
    VIEW_AUDIT_LOGS(7, "View Audit Logs"),

    // Applicable to all channel types
    VIEW_CHANNEL(10, "Read Text Channels & See Voice Channels"),

    // Text Permissions
    MESSAGE_READ(10, "Read Messages"),
    MESSAGE_WRITE(11, "Send Messages"),
    MESSAGE_TTS(12, "Send TTS Messages"),
    MESSAGE_MANAGE(13, "Manage Messages"),
    MESSAGE_EMBED_LINKS(14, "Embed Links"),
    MESSAGE_ATTACH_FILES(15, "Attach Files"),
    MESSAGE_HISTORY(16, "Read History"),
    MESSAGE_MENTION_EVERYONE(17, "Mention Everyone"),
    MESSAGE_EXT_EMOJI(18, "Use External Emojis"),

    // Voice Permissions
    VOICE_CONNECT(20, "Connect"),
    VOICE_SPEAK(21, "Speak"),
    VOICE_MUTE_OTHERS(22, "Mute Members"),
    VOICE_DEAF_OTHERS(23, "Deafen Members"),
    VOICE_MOVE_OTHERS(24, "Move Members"),
    VOICE_USE_VAD(25, "Use Voice Activity"),

    NICKNAME_CHANGE(26, "Change Nickname"),
    NICKNAME_MANAGE(27, "Manage Nicknames"),

    MANAGE_ROLES(28, "Manage Roles"),
    MANAGE_PERMISSIONS(28, "Manage Permissions"),
    MANAGE_WEBHOOKS(29, "Manage Webhooks"),
    MANAGE_EMOTES(30, "Manage Emojis");

    override val raw: Long = 1L shl offset

    override fun plus(other: IPermissionSet) = PermissionSet(raw or other.raw)
}

class PermissionSet(override val raw: Long) : IPermissionSet {
    override fun plus(other: IPermissionSet) = PermissionSet(raw or other.raw)
}

interface IPermissionSet {
    val raw: Long
    operator fun plus(other: IPermissionSet): PermissionSet

    infix fun has(other: IPermissionSet): Boolean = raw and other.raw == other.raw
    infix fun hasNot(other: IPermissionSet): Boolean = raw and other.raw != other.raw

    fun assertHas(other: IPermissionSet, message: String? = null) {
        if (this has other) return
        val missing = PermissionSet(getMissing(other.raw, this.raw))
        throw InsufficientPermissionException(missing, message
                ?: "Missing permissions: ${missing.asList()}")
    }

    fun asList(): List<Permission> {
        val list = mutableListOf<Permission>()
        Permission.values().forEach {
            if (it.raw and raw != 0L) list.add(it)
        }
        return list.toList()
    }
}

/** Performs converse nonimplication */
private fun getMissing(expected: Long, actual: Long) = (expected.inv() or actual).inv()
