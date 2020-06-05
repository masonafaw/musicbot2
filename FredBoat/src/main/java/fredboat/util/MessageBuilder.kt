package fredboat.util

import fredboat.sentinel.IMentionable

class MessageBuilder {

    companion object {
        internal val threadLocal = ThreadLocal.withInitial { MessageBuilder() }
    }

    private var builder = StringBuilder()
    val length: Int
        get() = builder.length

    fun append(str: String): MessageBuilder {
        builder.append(str)
        return this
    }

    fun append(bool: Boolean): MessageBuilder {
        builder.append(bool)
        return this
    }

    fun italic(str: String): MessageBuilder {
        builder.append("*").append(str).append("*")
        return this
    }

    fun bold(str: String): MessageBuilder {
        builder.append("**").append(str).append("**")
        return this
    }

    fun underlined(str: String): MessageBuilder {
        builder.append("__").append(str).append("__")
        return this
    }

    fun code(str: String): MessageBuilder {
        builder.append("`").append(str).append("`")
        return this
    }

    fun codeBlock(str: String, style: String = ""): MessageBuilder {
        builder.append("```")
                .append(style)
                .append("\n")
                .append(str)
                .append("\n```\n")
        return this
    }

    fun mention(mentionable: IMentionable): MessageBuilder {
        builder.append(mentionable.asMention)
        return this
    }

    fun clear(): MessageBuilder {
        builder = StringBuilder()
        return this
    }

    fun build(): String = builder.toString()

    override fun toString(): String = builder.toString()

}

fun localMessageBuilder() = MessageBuilder.threadLocal.get().clear()