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

package fredboat.feature.metrics;

import fredboat.main.BotController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by napster on 18.04.18.
 */
public class DockerStats {

    private static final Logger log = LoggerFactory.getLogger(DockerStats.class);
    private static final String BOT_IMAGE_STATS_URL = "https://hub.docker.com/v2/repositories/fredboat/fredboat/";
    private static final String DB_IMAGE_STATS_URL = "https://hub.docker.com/v2/repositories/fredboat/postgres/";

    private boolean fetched = false;
    private int dockerPullsBot;
    private int dockerPullsDb;

    void fetch() {
        try {
            dockerPullsBot = BotController.Companion.getHTTP().get(BOT_IMAGE_STATS_URL).asJson().getInt("pull_count");
            dockerPullsDb = BotController.Companion.getHTTP().get(DB_IMAGE_STATS_URL).asJson().getInt("pull_count");
            fetched = true;
        } catch (IOException e) {
            log.warn("Failed to fetch docker stats", e);
        }
    }

    /**
     * @return true if the stats have been fetched at least once successfully
     */
    public boolean isFetched() {
        return fetched;
    }

    //is 0 while uncalculated
    public int getDockerPullsBot() {
        return dockerPullsBot;
    }

    //is 0 while uncalculated
    public int getDockerPullsDb() {
        return dockerPullsDb;
    }
}
