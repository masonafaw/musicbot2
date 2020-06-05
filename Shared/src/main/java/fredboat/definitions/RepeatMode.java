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

package fredboat.definitions;

import java.util.Optional;

/**
 * Created by napster on 11.03.17.
 * <p>
 * Describes the possible repeat modes
 * <p>
 * OFF = no repeat is happening
 * SINGLE = the top most song in the queue is repeated
 * ALL = the whole queue is repeated.
 * <p>
 * Attention: This class or a copy of it might be mapped to a postgres enum in the backend. Make sure to write a
 * migration when changing it.
 */
public enum RepeatMode {
    OFF, SINGLE, ALL;

    /**
     * This method tries to parse an input into a repeat mode that we recognize.
     *
     * @param input input to be parsed into a repeat mode known to us (= defined in this enum)
     * @return the optional repeat mode identified from the input.
     */
    public static Optional<RepeatMode> parse(String input) {
        for (RepeatMode repeatMode : RepeatMode.values()) {
            if (repeatMode.name().equalsIgnoreCase(input)) {
                return Optional.of(repeatMode);
            }
        }
        return Optional.empty();
    }
}
