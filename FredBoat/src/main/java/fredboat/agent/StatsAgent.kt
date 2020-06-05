/*
 *
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
 */

package fredboat.agent

import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Created by napster on 11.10.17.
 *
 * Does stuff regularly to even out workload. Use this to register any hefty counts.
 */
class StatsAgent : FredBoatAgent {

    private val countActions = CopyOnWriteArrayList<Action>()
    private var runNumber = 0

    constructor(name: String, time: Int, timeUnit: TimeUnit) : super(name, time.toLong(), timeUnit)

    constructor(name: String) : super(name, 1, TimeUnit.MINUTES)

    fun addAction(action: Action) {
        countActions.add(action)
        try {
            action.act()
        } catch (e: Exception) {
            log.error("Unexpected exception when counting {}", action.name, e)
        }

    }

    override fun doRun() {
        runNumber++
        for (action in countActions) {
            try {
                if (runNumber % action.intervalMinutes == 0) action.act()
            } catch (e: Exception) {
                log.error("Unexpected exception when counting {}", action.name, e)
            }

        }
    }

    @FunctionalInterface
    interface Action {
        val name: String
            get() = "stats"
        val intervalMinutes: Int
            get() = 5

        @Throws(Exception::class)
        fun act()
    }

    class ActionAdapter(
            override val name: String,
            override val intervalMinutes: Int = 5,
            private val action: () -> Unit
    ) : Action {
        @Throws(Exception::class)
        override fun act() = action()
    }

    companion object {

        private val log = LoggerFactory.getLogger(StatsAgent::class.java)
    }

}
