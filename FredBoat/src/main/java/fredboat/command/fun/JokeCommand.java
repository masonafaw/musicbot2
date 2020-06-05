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

package fredboat.command.fun;

import fredboat.commandmeta.abs.CommandContext;
import fredboat.commandmeta.abs.IFunCommand;
import fredboat.commandmeta.abs.JCommand;
import fredboat.main.BotController;
import fredboat.messaging.internal.Context;
import fredboat.util.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

import static fredboat.main.LauncherKt.getBotController;

public class JokeCommand extends JCommand implements IFunCommand {

    private static final Logger log = LoggerFactory.getLogger(JokeCommand.class);

    public JokeCommand(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void onInvoke(@Nonnull CommandContext context) {
        //slow execution due to http requests
        getBotController().getExecutor().execute(() -> invoke(context));
    }

    public void invoke(@Nonnull CommandContext context) {
        try {
            JSONObject object = BotController.Companion.getHTTP().get("http://api.icndb.com/jokes/random").asJson();

            if (!"success".equals(object.getString("type"))) {
                throw new RuntimeException("Couldn't gather joke ;|");
            }
            
            String joke = object.getJSONObject("value").getString("joke");

            if (!context.getMentionedMembers().isEmpty()) {
                joke = joke.replaceAll("Chuck Norris", context.getMentionedMembers().get(0).getAsMention());
            } else if (context.hasArguments()) {
                joke = joke.replaceAll("Chuck Norris", context.getRawArgs());
            }
            
            joke = joke.replaceAll("&quot;", "\"");

            context.reply(TextUtils.escapeAndDefuse(joke));
        } catch (IOException | JSONException e) {
            log.error("Failed to fetch joke", e);
            context.reply(context.i18n("tryLater"));
        }
    }

    @Nonnull
    @Override
    public String help(@Nonnull Context context) {
        return "{0}{1} @<username>\n#Tell a joke about a user.";
    }
}
