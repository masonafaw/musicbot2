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

package fredboat.config.property;

import fredboat.shared.constant.DistributionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by napster on 03.03.18.
 */
@Component
@ConfigurationProperties(prefix = "config")
public class AppConfigProperties implements AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfigProperties.class);

    private boolean development = true;
    private boolean patron = true;

    private String prefix = "<<";
    private boolean restServerEnabled = false;
    private List<Long> botAdmins = new ArrayList<>();
    private List<Long> botOwners = new ArrayList<>();
    private boolean autoBlacklist = true;
    private String game = "";
    private boolean continuePlayback = false;
    private int shardCount = 1;
    //undocumented
    private int playerLimit = -1;
    private RatelimitConfig ratelimit;

    private boolean distributionLogged = false;

    @Override
    public DistributionEnum getDistribution() {
        DistributionEnum distribution = development ? DistributionEnum.DEVELOPMENT
                : patron ? DistributionEnum.PATRON
                : DistributionEnum.MUSIC;
        if (!distributionLogged) {
            log.info("Determined distribution: {}", distribution);
            distributionLogged = true;
        }
        return distribution;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public List<Long> getAdminIds() {
        return botAdmins;
    }

    @Override
    public List<Long> getOwnerIds() {
        return botOwners;
    }

    @Override
    public boolean useAutoBlacklist() {
        return autoBlacklist;
    }

    @Override
    public String getGame() {
        return game;
    }

    @Override
    public boolean getContinuePlayback() {
        return continuePlayback;
    }

    @Override
    public int getPlayerLimit() {
        return playerLimit;
    }

    @Override
    public int getShardCount() {
        return shardCount;
    }

    @Override
    public RatelimitConfig getRatelimit() {
        return ratelimit;
    }

    public void setDevelopment(boolean development) {
        this.development = development;
    }

    public void setPatron(boolean patron) {
        this.patron = patron;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        log.info("Using prefix: {}", prefix);
    }

    public void setRestServerEnabled(boolean restServerEnabled) {
        this.restServerEnabled = restServerEnabled;
    }

    public void setBotAdmins(List<Long> botAdmins) {
        this.botAdmins = botAdmins;
    }

    public void setBotOwners(List<Long> botOwners) {
        this.botOwners = botOwners;
    }

    public void setAutoBlacklist(boolean autoBlacklist) {
        this.autoBlacklist = autoBlacklist;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public void setContinuePlayback(boolean continuePlayback) {
        this.continuePlayback = continuePlayback;
    }

    public void setPlayerLimit(int playerLimit) {
        this.playerLimit = playerLimit;
    }

    public void setShardCount(int shardCount) {
        this.shardCount = shardCount;
    }

    public void setRatelimit(RatelimitConfig ratelimit) {
        this.ratelimit = ratelimit;
    }
}
