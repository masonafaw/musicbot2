/*
 * MIT License
 *
 * Copyright (c) 2017 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.command.`fun`

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IFunCommand
import fredboat.event.MessageEventHandler
import fredboat.messaging.internal.Context
import fredboat.sentinel.Guild
import fredboat.util.TextUtils
import fredboat.util.extension.edit
import io.prometheus.client.guava.cache.CacheMetricsCollector
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Function

class DanceCommand(cacheMetrics: CacheMetricsCollector, name: String, vararg aliases: String) : Command(name, *aliases), IFunCommand {

    private val locks: Function<Guild, ReentrantLock>

    private val allowed = Semaphore(5)

    init {
        val danceLockCache = CacheBuilder.newBuilder()
                .recordStats()
                .maximumSize(128) //any value will do, but not too big
                .build<Long, ReentrantLock>(CacheLoader.from<ReentrantLock> { ReentrantLock() })
        cacheMetrics.addCache("danceLockCache", danceLockCache)
        locks = danceLockCache.compose { it.id } //mapping guild id to a lock
    }

    override suspend fun invoke(context: CommandContext) {
        //locking by use of java.util.concurrent.locks

        //in most cases we would need to use a fair lock, but since
        // any one lock is only set-up by one thread we can get away with a naive isLocked check
        val lock = locks.apply(context.guild)
        if (lock.isLocked || !allowed.tryAcquire()) {
            //already in progress or not allowed
            context.reply(context.i18n("tryLater"))
            return
        }

        context.replyMono(TextUtils.ZERO_WIDTH_CHAR + "\\o\\")
                .delayElement(Duration.ofSeconds(1))
                .subscribe { response ->
                    val func = Runnable {
                        try {
                            lock.lock()
                            MessageEventHandler.messagesToDeleteIfIdDeleted.put(context.msg.id, response.messageId)
                            val start = System.currentTimeMillis()
                            while (start + 60000 > System.currentTimeMillis()) {
                                Thread.sleep(1000)
                                response.edit(context.textChannel, "/o/").block(Duration.ofSeconds(2))
                                Thread.sleep(1000)
                                response.edit(context.textChannel, "\\o\\").block(Duration.ofSeconds(2))
                            }
                        } catch (ignored: TimeoutException) {
                        } catch (ignored: ExecutionException) {
                        } catch (ignored: InterruptedException) {
                        } finally {
                            allowed.release()
                            lock.unlock()
                        }
                    }

                    val thread = Thread(func, DanceCommand::class.java.simpleName + " dance")
                    thread.start()
                }
    }

    override fun help(context: Context): String {
        return "{0}{1}\n#Dance for a minute."
    }
}
