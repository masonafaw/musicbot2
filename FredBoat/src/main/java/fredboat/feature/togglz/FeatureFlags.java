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

package fredboat.feature.togglz;

import org.togglz.core.Feature;
import org.togglz.core.annotation.Label;

/**
 * Created by napster on 19.05.17.
 * <p>
 * Implementation of the feature flag pattern
 */
public enum FeatureFlags implements Feature {

    @Label("Patron validation")
    PATRON_VALIDATION,

    @Label("Force soundcloud search instead of youtube")
    FORCE_SOUNDCLOUD_SEARCH,

    @Label("Disable the use of the Youtube API with ;;nowplaying")
    DISABLE_NOWPLAYING_WITH_YTAPI,

    @Label("When YouTube fails to load, display link to block notice")
    SHOW_YOUTUBE_RATELIMIT_WARNING

    ;

    public boolean isActive() {
        return FeatureConfig.getTheFeatureManager().isActive(this);
    }
}
