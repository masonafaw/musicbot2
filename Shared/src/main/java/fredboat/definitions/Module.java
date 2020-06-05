/*
 *
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

package fredboat.definitions;

import fredboat.util.Emojis;

import java.util.Optional;

/**
 * Created by napster on 15.02.18.
 * <p>
 * A user should not be able to enable/disable a locked module
 */
public enum Module {

    //@formatter:off                               locked
    //                                    enabledByDef
    ADMIN ("moduleAdmin",      Emojis.KEY,    true, true, "administration"),
    INFO  ("moduleInfo",       Emojis.INFO,   true, true, "information"),
    CONFIG("moduleConfig",     Emojis.GEAR,   true, true, "configuration"),
    MUSIC ("moduleMusic",      Emojis.MUSIC,  true, true, "music"),
    MOD   ("moduleModeration", Emojis.HAMMER, true, false, "moderation"),
    UTIL  ("moduleUtility",    Emojis.TOOLS,  true, false, "utility"),
    FUN   ("moduleFun",        Emojis.DIE,    true, false, "fun"),
    ;
    //@formatter:on

    private final String translationKey;
    private final String emoji;
    private final boolean enabledByDefault;
    private final boolean lockedModule;
    private final String altName;

    Module(String translationKey, String emoji, boolean enabledByDefault, boolean lockedModule,
           String altName) {
        this.translationKey = translationKey;
        this.emoji = emoji;
        this.enabledByDefault = enabledByDefault;
        this.lockedModule = lockedModule;
        this.altName = altName;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getEmoji() {
        return emoji;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public boolean isLockedModule() {
        return lockedModule;
    }

    public String getAltName() {
        return altName;
    }

    /**
     * This method tries to parse an input into a module that we recognize.
     *
     * @param input input to be parsed into a Module known to us (= defined in this enum)
     * @return the optional module identified from the input.
     */
    public static Optional<Module> parse(String input) {
        for (Module module : Module.values()) {
            if (module.name().equalsIgnoreCase(input)
                    || module.getAltName().equalsIgnoreCase(input)) {
                return Optional.of(module);
            }
        }

        return Optional.empty();
    }
}
