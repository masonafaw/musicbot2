/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
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

package fredboat.commandmeta

import fredboat.testutil.IntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Created by napster on 22.03.17.
 *
 *
 * Tests for command initialization
 */
class CommandInitializerTest : IntegrationTest() {

    /**
     * Make sure all commands initialized in the bot provide help
     */
    @Test
    fun testHelpStrings(@Suppress("UNUSED_PARAMETER") initializer: CommandInitializer) {
        for (c in CommandRegistry.getAllRegisteredCommandsAndAliases()) {
            val com = CommandRegistry.findCommand(c)!!
            Assertions.assertNotNull(com, "Command looked up by $c is null")
            val ctx = commandTester.parseContext(";; help ${com.name}")
            val help = com.help(ctx)
            Assertions.assertNotNull(help) { com.javaClass.name + ".help() returns null" }
            Assertions.assertNotEquals("", help) { com.javaClass.name + ".help() returns an empty string" }
        }
    }
}
