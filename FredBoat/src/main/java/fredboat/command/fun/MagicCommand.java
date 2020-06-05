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

package fredboat.command.fun;

import fredboat.commandmeta.abs.JCommand;
import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.messaging.internal.Context;
import fredboat.util.AsciiArtConstant;
import fredboat.util.TextUtils;

import javax.annotation.Nonnull;

/**
 * Created by napster on 24.12.17.
 */
public class MagicCommand extends JCommand implements IFunCommand {
    public MagicCommand(@Nonnull String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        String message = "ABRA KADABRA...";
        if (context.hasArguments()) {
            message = TextUtils.defuse(context.getRawArgs());
        }

        context.reply(AsciiArtConstant.MAGICAL_LENNY + message);
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} [message]\n#This command is magic ( ͡° ͜ʖ ͡°)";
    }
}
