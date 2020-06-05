package fredboat.commandmeta.abs

import fredboat.definitions.Module
import fredboat.messaging.internal.Context
import java.util.*

abstract class Command protected constructor(val name: String, vararg aliases: String) {
    val aliases: List<String> = Arrays.asList(*aliases)
    var module: Module? = null

    abstract suspend operator fun invoke(context: CommandContext)

    /**
     * @param context Context for where the help is going to be posted, mostly used for i18ning the help string
     * @return an unformatted help string: convention {0} = prefix, {1} = command, fill these in by the running bot, more parameters can be present
     */
    abstract fun help(context: Context): String

    override fun equals(other: Any?): Boolean {
        return other is Command && other.name == this.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}