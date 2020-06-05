package fredboat.command.info

import com.fredboat.sentinel.entities.SentinelHello
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IInfoCommand
import fredboat.messaging.internal.Context
import fredboat.sentinel.Sentinel
import fredboat.util.MessageBuilder
import kotlinx.coroutines.reactive.awaitLast
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class SentinelsCommand(name: String, vararg aliases: String) : Command(name, *aliases), IInfoCommand {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SentinelsCommand::class.java)
    }

    override suspend fun invoke(context: CommandContext) {
        context.sendTyping()

        val msg = MessageBuilder()
        val compounds = context.sentinel.tracker.sentinels
                .asSequence()
                .sortedBy { it.shardStart }
                .map { SentinelDataCompound(it) }
                .toList()

        try {
            context.sentinel.getAllSentinelInfo(includeShards = true)
                    .timeout(Duration.ofSeconds(5))
                    .doOnNext { res ->
                        compounds.find { it.hello.key == res.routingKey }?.apply {
                            data = res
                        }
                    }
                    .awaitLast()
        } catch (e: Exception) {
            log.error("Exception while gathering sentinel data", e)
        }

        if (context.trigger == "sentisneks") {
            msg.append("*Hiss* :snake:\n")
        }

        msg.append("```diff\n")
        compounds.forEachIndexed { i, it ->
            msg.append(it.toString(i))
        }
        msg.append("```\n")
        context.reply(msg.build())
    }

    private class SentinelDataCompound(val hello: SentinelHello) {
        lateinit var data: Sentinel.NamedSentinelInfoResponse

        fun toString(i: Int): String {
            hello.run {
                return if (::data.isInitialized) {
                    "+ $i [$shardStart..$shardEnd/$shardCount] -- key: $key, guilds ${data.response.guilds}\n"
                } else {
                    "- $i [$shardStart..$shardEnd/$shardCount] -- key: $key\n"
                }
            }
        }
    }

    override fun help(context: Context): String {
        return "{0}{1} [full]\n#Show information about the sentinels in a detailed report."
    }

}