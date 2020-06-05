package fredboat.command.info

import com.fredboat.sentinel.entities.GetPingReponse
import com.fredboat.sentinel.entities.GetPingRequest
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.messaging.internal.Context
import kotlinx.coroutines.reactive.awaitFirst
import kotlin.math.roundToInt

/**
 * Created by epcs on 6/30/2017.
 * Good enough of an indicator of the ping to Discord.
 */

class PingCommand(name: String, vararg aliases: String) : Command(name, *aliases), IInfoCommand {

    override fun help(context: Context): String {
        return "{0}{1}\n#Return the ping to Discord."
    }

    override suspend fun invoke(context: CommandContext) {
        val response = context.sentinel.send<GetPingReponse>(context.routingKey, GetPingRequest(context.guild.shardId))
                .awaitFirst()
        context.reply(
                "Ping of shard ${context.guild.shardId}: ${response.shardPing}ms, " +
                        "Average: ${response.average.roundToInt()}ms"
        )
    }
}

//hello
//this is a comment
//I want pats
//multiple pats
//pats never seen before
