package fredboat.command.admin

import com.fredboat.sentinel.entities.EvalRequest
import com.fredboat.sentinel.entities.SentinelHello
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.ICommandRestricted
import fredboat.definitions.PermissionLevel
import fredboat.messaging.internal.Context
import fredboat.util.MessageBuilder
import fredboat.util.extension.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RemoteEvalCommand(
        name: String,
        vararg aliases: String
) : Command(name, *aliases), ICommandRestricted {

    companion object {
        private const val MAX_UPDATES = 1000
    }

    override val minimumPerms = PermissionLevel.BOT_OWNER
    private var onKill: (() -> Unit)? = null

    override suspend fun invoke(context: CommandContext) {
        val parsed = parse(context) ?: return
        val (sentinels, request) = parsed

        val monos = sentinels.map {
            context.sentinel.send<String>(it.key, request).onErrorResume { ex ->
                Mono.just("Request failed: ${ex.message}")
            }.map { resultString -> it to resultString }
        }

        val results = sentinels.map { EvalResult(it) }
        var completed = false
        val timeStart = System.currentTimeMillis()
        val getElapsed = { System.currentTimeMillis() - timeStart }
        val disposable = Flux.merge(monos)
                .doFinally { completed = true }
                .subscribe { pair ->
                    results.find { it.sentinel == pair.first }!!.apply {
                        elapsedMs = getElapsed()
                        response = pair.second
                    }
                }

        onKill = { disposable.dispose() }

        val message = context.replyMono(generateOutput(results, getElapsed())).awaitSingle()
        delay(1000L)
        var updates = 0
        while (!completed && MAX_UPDATES >= updates) {
            updates++
            message.edit(context.msg.channel, generateOutput(results, getElapsed())).subscribe()
            delay(1000L)
        }
        message.edit(context.msg.channel, generateOutput(results, getElapsed())).subscribe()
    }

    private fun parse(context: CommandContext): ParseResult? {
        val raw = context.msg.raw.content
        val firstLine = raw.substringBefore("\n")
        val source = raw.substringAfter("\n")

        if (firstLine == raw && !raw.contains("-k") && !raw.contains("--kill") ) {
            HelpCommand.sendFormattedCommandHelp(context)
            return null
        }

        val sentinels = context.sentinel.tracker.sentinels.sortedBy { it.shardStart }

        var kill = false
        var requestAll = false
        var lastTokenWasTimeout = false
        var timeout: Int? = null
        // Parse flags and sentinels
        var selectedSentinels = firstLine.splitToSequence(" ")
                // Handle non-sentinel references, and filter them out
                .filter {
                    if (lastTokenWasTimeout) {
                        timeout = it.toInt()
                        return@filter false
                    }

                    when (it) {
                        "-k", "--kill" -> {
                            kill = true;false
                        }
                        "-t", "--timeout" -> {
                            lastTokenWasTimeout = true; false
                        }
                        "-a", "--all" -> {
                            requestAll = true; false
                        }
                        else -> true
                    }
                }
                // Map tokens to sentinels
                .mapNotNull {
                    it.toIntOrNull()?.let { num ->
                        sentinels.getOrNull(num).let { hello -> return@mapNotNull hello }
                    }
                    sentinels.find { hello -> hello.key == it }
                }.toSet()

        if (requestAll) selectedSentinels = sentinels.asSequence().sortedBy { it.shardStart }.toSet()

        return ParseResult(selectedSentinels, EvalRequest(
                source, timeout, kill
        ))
    }

    private fun generateOutput(results: List<EvalResult>, elapsedMs: Long): String {
        val msg = MessageBuilder()
        msg.append("**Eval status:**\n")
        results.forEach { result ->
            if (result.elapsedMs == null) {
                msg.append("${result.sentinel.key}: <pending>\n")
            } else {
                msg.append("${result.sentinel.key}: ${result.response} (${result.elapsedMs}ms)\n")
            }
        }

        msg.append("\n${elapsedMs}ms elapsed")
        return msg.build()
    }

    private data class ParseResult(val sentinels: Set<SentinelHello>, val request: EvalRequest)
    private data class EvalResult(
            val sentinel: SentinelHello,
            var elapsedMs: Long? = null,
            var response: String? = null
    )

    override fun help(context: Context): String {
        return ("{0}{1} [-t seconds | -k | -a] <sentinel-keys-or-numbers> \\n <kotlin-code>"
                + " #Run the provided kotlin code on the selected sentinel."
                + " By default no timeout is set for the task, set a timeout by passing `-t` as the first argument and"
                + " the amount of seconds to wait for the task to finish as the second argument."
                + " Run with `-k` or `--kill` as first argument to stop the last submitted eval task if it's still ongoing.")
    }

}