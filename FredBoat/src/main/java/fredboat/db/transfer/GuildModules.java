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

package fredboat.db.transfer;

import fredboat.definitions.Module;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by napster on 20.03.18.
 * <p>
 * Transfer object for the {@link fredboat.db.entity.main.GuildModules}
 */
//todo move ""business"" logic to the backend
public class GuildModules implements TransferObject<Long> {

    private long guildId;
    @Nullable
    private Boolean adminModule;
    @Nullable
    private Boolean infoModule;
    @Nullable
    private Boolean configModule;
    @Nullable
    private Boolean musicModule;
    @Nullable
    private Boolean modModule;
    @Nullable
    private Boolean utilModule;
    @Nullable
    private Boolean funModule;

    @Override
    public void setId(Long guildId) {
        this.guildId = guildId;
    }

    @Override
    public Long getId() {
        return guildId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.guildId);
    }

    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof GuildModules) && ((GuildModules) obj).guildId == this.guildId;
    }

    @CheckReturnValue
    public GuildModules enableModule(Module module) {
        return setModule(module, true);
    }

    @CheckReturnValue
    public GuildModules disableModule(Module module) {
        return setModule(module, false);
    }


    @CheckReturnValue
    public GuildModules resetModule(Module module) {
        return setModule(module, null);
    }

    @CheckReturnValue
    private GuildModules setModule(Module module, @Nullable Boolean enabled) {
        switch (module) {
            case ADMIN:
                adminModule = enabled;
                break;
            case INFO:
                infoModule = enabled;
                break;
            case CONFIG:
                configModule = enabled;
                break;
            case MUSIC:
                musicModule = enabled;
                break;
            case MOD:
                modModule = enabled;
                break;
            case UTIL:
                utilModule = enabled;
                break;
            case FUN:
                funModule = enabled;
                break;
            default:
                throw new RuntimeException("Unknown Module " + module.name());
        }
        return this;
    }

    @Nullable
    public Boolean isModuleEnabled(Module module) {
        switch (module) {
            case ADMIN:
                return adminModule;
            case INFO:
                return infoModule;
            case CONFIG:
                return configModule;
            case MUSIC:
                return musicModule;
            case MOD:
                return modModule;
            case UTIL:
                return utilModule;
            case FUN:
                return funModule;
            default:
                throw new RuntimeException("Unknown Module " + module.name());
        }
    }

    /**
     * @return true if the provided module is enabled, false if not. If no value has been specified, return the provide
     * default value.
     */
    public boolean isModuleEnabled(Module module, boolean def) {
        Boolean enabled = isModuleEnabled(module);
        if (enabled != null) {
            return enabled;
        } else {
            return def;
        }
    }

    public List<Module> getEnabledModules() {
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : Module.values()) {
            if (isModuleEnabled(module, module.isEnabledByDefault())) {
                enabledModules.add(module);
            }
        }
        return enabledModules;
    }
}
